package searchengine.indexer.queue;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.indexer.IndexerType;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.dto.indexing.LemmaIndexCouple;
import searchengine.statistic.SiteStatisticUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;


public class PageRepositoryQueueService implements Runnable {
    private final PageRepository pageRepository;
    private final IndexerType indexerType;
    private final Thread parserThread;
    private final BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository;
    public PageRepositoryQueueService(PageRepository pageRepository,
                                      Thread parserThread,
                                      IndexerType indexerType,
                                      BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository,
                                      BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository) {
        this.pageRepository = pageRepository;
        this.parserThread = parserThread;
        this.indexerType = indexerType;
        this.pageEntityQueueForPageRepository = pageEntityQueueForPageRepository;
        this.lemmasEntityQueueForLemmasRepository = lemmasEntityQueueForLemmasRepository;
    }

    @Override
    public void run() {
        while (parserThread.isAlive() || !pageEntityQueueForPageRepository.isEmpty()) {
            if (pageEntityQueueForPageRepository.isEmpty()) {
                continue;
            }
            int bath = indexerType.getPageBatchSize();
            List<LemmaIndexCouple> batchToProcess = batchToProcess(bath);
            List<PageEntity> listToWrite = collectPageEntityList(batchToProcess);
            pageRepository.saveAll(listToWrite);
            try {
                lemmasEntityQueueForLemmasRepository.put(batchToProcess);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private List<LemmaIndexCouple> batchToProcess (int batch) {
        List<LemmaIndexCouple> lemmaIndexCoupleList = new ArrayList<>();
        for (int i = 0; i < batch; i++) {
            LemmaIndexCouple lemmaIndexCouple = pageEntityQueueForPageRepository.poll();
            if (lemmaIndexCouple != null) {
                lemmaIndexCoupleList.add(lemmaIndexCouple);
                continue;
            }
            if (!parserThread.isAlive()) {
                break;
            } else {
                i--;
            }
        }
        return lemmaIndexCoupleList;
    }
    private List<PageEntity> collectPageEntityList (List<LemmaIndexCouple> batchToProcess) {
        List<PageEntity> pageEntityList = new ArrayList<>();
        for (LemmaIndexCouple couple : batchToProcess) {
            pageEntityList.add(couple.getPageEntity());
        }
        return pageEntityList;
    }

}
