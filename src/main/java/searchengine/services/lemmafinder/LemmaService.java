package searchengine.services.lemmafinder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.model.page.PageEntity;
import searchengine.services.dto.CollectedLemmas;
import searchengine.services.dto.LemmaIndexCouple;
import java.util.*;
import java.util.concurrent.BlockingQueue;


@Setter
@Getter
@Service
public class LemmaService implements LemmaFinderService {
    private final BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor;
    private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;
    private final Thread parserThread;
    private final LemmaFinder lemmaFinder;
    private static int processedPagesForLemma = 0;
    private static int sumFrequencyLemmas = 0;
    public LemmaService(Thread parserThread,
                        BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor,
                        BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService)  {
        this.parserThread = parserThread;
        this.queueForDataProcessor = queueForDataProcessor;
        this.pageEntityQueueForLemmaService = pageEntityQueueForLemmaService;
        lemmaFinder = LemmaFinder.getInstance();
    }
    @Override
    public void runLemmatization()  {
        while (parserThread.isAlive() || !pageEntityQueueForLemmaService.isEmpty()) {
            if (pageEntityQueueForLemmaService.isEmpty()) {
                continue;
            }
            List<LemmaIndexCouple> lemmaIndexCoupleList = new ArrayList<>();
            List<PageEntity> pageEntityList = pageEntityQueueForLemmaService.poll();

            if (pageEntityList != null && !pageEntityList.isEmpty()) {
                for (PageEntity page : pageEntityList) {
                    LemmaIndexCouple syncObject = new LemmaIndexCouple();
                    CollectedLemmas collectedLemmas = new CollectedLemmas();
                    processPageEntityList(page, collectedLemmas);
                    syncObject.setPageEntity(page);
                    syncObject.setCollectedLemmas(collectedLemmas);
                    lemmaIndexCoupleList.add(syncObject);
                }
            }
            queueForDataProcessor.add(lemmaIndexCoupleList);
        }
    }
    private void processPageEntityList(PageEntity page,
                                       CollectedLemmas collectedLemmas) {
        if (page != null) {
            String text = page.getContent();
            Map<String, Integer> processedPage = lemmaFinder.collectLemmas(text);
            collectedLemmas.setPageEntity(page);
            collectedLemmas.setCollectedLemmas(processedPage);
        }
    }
}
