package searchengine.services.queue;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.services.dto.LemmaIndexCouple;
import searchengine.services.parser.IndexRatioModel;
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
    private final DetailedStatisticsItem siteStatistic;
    public LemmaRepositoryQueueService(LemmaRepository lemmaRepository,
                                       IndexRepository indexRepository,
                                       SiteRepository siteRepository,
                                       SiteEntity siteEntity,
                                       Thread parserThread,
                                       IndexRatioModel indexRatioModel,
                                       DetailedStatisticsItem siteStatistic,
                                       BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository) {
        this.parserThread = parserThread;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        this.indexRatioModel = indexRatioModel;
        this.siteStatistic = siteStatistic;
        this.lemmasEntityQueueForLemmasRepository = lemmasEntityQueueForLemmasRepository;
    }
    @Override
    public void run() {
        System.out.println("Начало - LemmaRepository - " + Thread.currentThread().getName());
        while (parserThread.isAlive() || !lemmasEntityQueueForLemmasRepository.isEmpty()) {
            if (lemmasEntityQueueForLemmasRepository.isEmpty()) {
                continue;
            }
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            List<IndexEntity> indexEntityList = new ArrayList<>();
            createBatchToWrite(lemmaEntityList, indexEntityList);
            try {
                performWritingToDB(lemmaEntityList, indexEntityList);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            siteStatistic.setStatusTime(System.currentTimeMillis());
        }
        siteTableUpdateOnFinish();
        System.out.println("Конец - LemmaRepository - " + Thread.currentThread().getName());
    }
    private void performWritingToDB(List<LemmaEntity> lemmaEntityList,
                                    List<IndexEntity> indexEntityList) throws InterruptedException {
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
    public void siteTableUpdateOnFinish() {
        siteEntity.setLastError(indexRatioModel.getIndexRatioModelMessage());
        siteEntity.setStatus(indexRatioModel.getIndexingStatus());
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
        siteStatistic.setStatus(indexRatioModel.getIndexingStatus().toString());
    }
}
