package searchengine.services.queue;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.services.dto.LemmaIndexCouple;
import searchengine.services.ini.ParserType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;


public class PageRepositoryQueueService implements Runnable {
    private final PageRepository pageRepository;
    private final ParserType parserType;
    private final Thread parserThread;
    private final BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository;
    private final DetailedStatisticsItem siteStatistic;
    public PageRepositoryQueueService(PageRepository pageRepository,
                                      Thread parserThread,
                                      ParserType parserType,
                                      BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository,
                                      BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository,
                                      DetailedStatisticsItem siteStatistic) {
        this.pageRepository = pageRepository;
        this.parserThread = parserThread;
        this.parserType = parserType;
        this.pageEntityQueueForPageRepository = pageEntityQueueForPageRepository;
        this.lemmasEntityQueueForLemmasRepository = lemmasEntityQueueForLemmasRepository;
        this.siteStatistic = siteStatistic;
    }

    @Override
    public void run() {
        if (parserType == ParserType.MULTIPLESITES || parserType == ParserType.SINGLESITE) {
            multiBatchProcessing();
        } else if (parserType == ParserType.SINGLEPAGE) {
            singleBatchProcessing();
        }

    }
    private void multiBatchProcessing() {
        while (parserThread.isAlive() || !pageEntityQueueForPageRepository.isEmpty()) {
            if (pageEntityQueueForPageRepository.isEmpty()) {
                continue;
            }
            List<LemmaIndexCouple> batchToProcess = batchToProcess(50);
            List<PageEntity> listToWrite = collectPageEntityList(batchToProcess);
            pageRepository.saveAll(listToWrite);
            try {
                lemmasEntityQueueForLemmasRepository.put(batchToProcess);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            siteStatistic.incrementPages(batchToProcess.size());
        }
    }
    private void singleBatchProcessing() {
        while (parserThread.isAlive() || !pageEntityQueueForPageRepository.isEmpty()) {
            if (pageEntityQueueForPageRepository.isEmpty()) {
                continue;
            }
            List<LemmaIndexCouple> batchToProcess = batchToProcess(1);
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
