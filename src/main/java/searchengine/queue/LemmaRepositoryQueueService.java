package searchengine.queue;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.site.IndexingStatus;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.dto.indexing.LemmaIndexCouple;
import searchengine.indexer.ParserType;
import searchengine.parser.IndexRatioModel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
public class LemmaRepositoryQueueService implements Runnable {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final SiteEntity siteEntity;
    private final IndexRatioModel indexRatioModel;
    private final Thread parserThread;
    private final BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository;
    private final ParserType parserType;
    private final DetailedStatisticsItem siteStatistic;
    public LemmaRepositoryQueueService(LemmaRepository lemmaRepository,
                                       IndexRepository indexRepository,
                                       SiteRepository siteRepository,
                                       SiteEntity siteEntity,
                                       Thread parserThread,
                                       IndexRatioModel indexRatioModel,
                                       DetailedStatisticsItem siteStatistic,
                                       BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository,
                                       ParserType parserType) {
        this.parserThread = parserThread;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.indexRatioModel = indexRatioModel;
        this.siteStatistic = siteStatistic;
        this.lemmasEntityQueueForLemmasRepository = lemmasEntityQueueForLemmasRepository;
        this.parserType = parserType;
    }
    @Override
    public void run() {
        while (parserThread.isAlive() || !lemmasEntityQueueForLemmasRepository.isEmpty()) {
            if (lemmasEntityQueueForLemmasRepository.isEmpty()) {
                continue;
            }
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            List<IndexEntity> indexEntityList = new ArrayList<>();
            createBatchToWrite(lemmaEntityList, indexEntityList);
            performWritingToDB(lemmaEntityList, indexEntityList);
            if (parserType != ParserType.SINGLEPAGE) {
                siteStatistic.setStatusTime(System.currentTimeMillis());
            }
        }
        siteTableUpdateOnFinish();
    }
    private void performWritingToDB(List<LemmaEntity> lemmaEntityList,
                                    List<IndexEntity> indexEntityList) {
        lemmaRepository.saveAll(lemmaEntityList);
        indexRepository.saveAll(indexEntityList);
    }
    private void createBatchToWrite (List<LemmaEntity> lemmaEntityList,
                                        List<IndexEntity> indexEntityList) {
        List<LemmaIndexCouple> lemmaIndexCoupleList = lemmasEntityQueueForLemmasRepository.poll();
        if (lemmaIndexCoupleList != null) {
            for (LemmaIndexCouple couple : lemmaIndexCoupleList) {
                lemmaEntityList.addAll(couple.getLemmaEntityList());
                indexEntityList.addAll(couple.getIndexEntityList());
            }
        }
    }
    private void siteTableUpdateOnFinish() {
        siteEntity.setLastError(indexRatioModel.getIndexRatioModelMessage());
        siteEntity.setStatus(IndexingStatus.INDEXED);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteRepository.save(siteEntity);
        if (parserType != ParserType.SINGLEPAGE) {
            siteStatistic.setStatus("INDEXED");
        }
    }
}
