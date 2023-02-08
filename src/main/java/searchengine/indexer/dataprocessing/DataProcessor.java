package searchengine.indexer.dataprocessing;
import searchengine.dto.indexing.CollectedLemmas;
import searchengine.dto.indexing.Index;
import searchengine.indexer.IndexerType;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexKey;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;
import searchengine.dto.indexing.LemmaIndexCouple;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
public class DataProcessor implements DataProcessingService {
    private final BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor;
    private final Map<String, LemmaEntity> redisLemmas;
    private final SiteEntity siteEntity;
    private final Thread parserThread;

    public DataProcessor(Thread parserThread,
                         SiteEntity siteEntity,
                         BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository,
                         BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor,
                         IndexerType indexerType) {
        this.parserThread = parserThread;
        this.siteEntity = siteEntity;
        this.pageEntityQueueForPageRepository = pageEntityQueueForPageRepository;
        this.queueForDataProcessor = queueForDataProcessor;
        this.redisLemmas = new TempLemmaStorage(indexerType, siteEntity).getTempStorage();
    }
    @Override
    public void run() {
        while (parserThread.isAlive() || !queueForDataProcessor.isEmpty()) {
            List<LemmaIndexCouple> pageLemmaIndexCouple = queueForDataProcessor.poll();
            if (pageLemmaIndexCouple == null) {
                continue;
            }
            multiplePageProcessor(pageLemmaIndexCouple);
        }
    }
    private void multiplePageProcessor(List<LemmaIndexCouple> pageLemmaIndexCouple) {
        for (LemmaIndexCouple couple : pageLemmaIndexCouple) {
            List<Index> indexes = new ArrayList<>();
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            singlePageProcessor(couple, lemmaEntityList, indexes);
            couple.setLemmaCount(redisLemmas.size());
        }
    }
    private void singlePageProcessor(LemmaIndexCouple pageLemmaIndexCouple,
                                     List<LemmaEntity> lemmaEntityList,
                                     List<Index> indexes){
        CollectedLemmas lemmas = pageLemmaIndexCouple.getCollectedLemmas();
        PageEntity pageEntity = lemmas.getPageEntity();
        Map<String, Integer> collectedLemmas = lemmas.getCollectedLemmas();
        multiplePageCollectedLemmasProcessing(collectedLemmas, lemmaEntityList, pageEntity, indexes);
        List<IndexEntity> indexesForRepository = prepareIndexesForRepository(indexes);
        if (lemmaEntityList.size() != 0) {
            pageLemmaIndexCouple.setLemmaEntityList(lemmaEntityList);
            pageLemmaIndexCouple.setIndexEntityList(indexesForRepository);
            try {
                pageEntityQueueForPageRepository.put(pageLemmaIndexCouple);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private List<IndexEntity> prepareIndexesForRepository(List<Index> indexes) {
        List<IndexEntity> indexEntityList = new ArrayList<>();
        indexes.forEach(entry -> {
            IndexEntity index = new IndexEntity();
            IndexKey indexKey = new IndexKey(entry.pageEntity(), entry.lemmaEntity());
            index.setIndex_id(indexKey);
            index.setRank(entry.rating());
            indexEntityList.add(index);
        });
        return indexEntityList;
    }
    private void multiplePageCollectedLemmasProcessing (Map<String, Integer> collectedLemmas,
                                                        List<LemmaEntity> lemmaEntityList,
                                                        PageEntity pageEntity, List<Index> indexes) {
        siteEntity.setStatusTime(LocalDateTime.now());
        collectedLemmas.forEach((key, value) -> {
            LemmaEntity lemmaEntity;
            if (redisLemmas.containsKey(key)) {
                lemmaEntity = redisLemmas.get(key);
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            } else {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setLemma(key);
                lemmaEntity.setFrequency(1);
            }

            lemmaEntity.getSite().setStatusTime(LocalDateTime.now());
            redisLemmas.put(key, lemmaEntity);
            lemmaEntityList.add(lemmaEntity);
            Index index = new Index(pageEntity, lemmaEntity, value);
            indexes.add(index);
        });
    }
}
