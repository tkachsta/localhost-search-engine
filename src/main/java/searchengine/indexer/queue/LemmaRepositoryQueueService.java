package searchengine.indexer.queue;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.site.IndexingStatus;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.dto.indexing.LemmaIndexCouple;
import searchengine.indexer.IndexerType;
import searchengine.indexer.parser.IndexRatioModel;
import searchengine.statistic.SiteStatisticUpdate;
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
    private final SiteStatisticUpdate siteStatisticUpdate;
    public LemmaRepositoryQueueService(LemmaRepository lemmaRepository,
                                       IndexRepository indexRepository,
                                       SiteRepository siteRepository,
                                       SiteEntity siteEntity,
                                       Thread parserThread,
                                       IndexRatioModel indexRatioModel,
                                       DetailedStatisticsItem siteStatistic,
                                       BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository,
                                       IndexerType indexerType) {
        this.parserThread = parserThread;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.indexRatioModel = indexRatioModel;
        this.lemmasEntityQueueForLemmasRepository = lemmasEntityQueueForLemmasRepository;
        this.siteStatisticUpdate = new SiteStatisticUpdate(indexerType, siteStatistic);
    }
    @Override
    public void run() {
        while (parserThread.isAlive() || !lemmasEntityQueueForLemmasRepository.isEmpty()) {
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            List<IndexEntity> indexEntityList = new ArrayList<>();
            if (lemmasEntityQueueForLemmasRepository.isEmpty()) {
                continue;
            }
            List<LemmaIndexCouple> lemmaIndexCoupleList = lemmasEntityQueueForLemmasRepository.poll();
            if (lemmaIndexCoupleList != null) {
                for (LemmaIndexCouple couple : lemmaIndexCoupleList) {
                    lemmaEntityList.addAll(couple.getLemmaEntityList());
                    indexEntityList.addAll(couple.getIndexEntityList());
                }
                LemmaIndexCouple max = lemmaIndexCoupleList.stream()
                        .max(Comparator.comparing(LemmaIndexCouple::getLemmaCount))
                        .orElseThrow(NoSuchElementException::new);
                int lemmaCount = max.getLemmaCount();
                int pageCount = lemmaIndexCoupleList.size();
                performWritingToDB(lemmaEntityList, indexEntityList, lemmaCount, pageCount);
                siteStatisticUpdate.updateCurrentTime();
            }
        }
        siteStatisticUpdate.updateStatus(IndexingStatus.INDEXED);
        siteTableUpdateOnFinish();
    }
    private void performWritingToDB(List<LemmaEntity> lemmaEntityList,
                                    List<IndexEntity> indexEntityList,
                                    int lemmaCount, int pageCount) {
        lemmaRepository.saveAll(lemmaEntityList);
        indexRepository.saveAll(indexEntityList);
        synchronized (siteStatisticUpdate) {
            siteStatisticUpdate.updatePageCount(pageCount);
            siteStatisticUpdate.updateLemmaCount(lemmaCount);
        }
    }
    private void siteTableUpdateOnFinish() {
        siteEntity.setLastError(indexRatioModel.getIndexRatioModelMessage());
        siteEntity.setStatus(IndexingStatus.INDEXED);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError("");
        siteRepository.save(siteEntity);
    }
}
