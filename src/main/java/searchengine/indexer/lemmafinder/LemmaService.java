package searchengine.indexer.lemmafinder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.model.page.PageEntity;
import searchengine.dto.indexing.CollectedLemmas;
import searchengine.dto.indexing.LemmaIndexCouple;
import java.util.*;
import java.util.concurrent.BlockingQueue;


@Setter
@Getter
@Component
@AllArgsConstructor
public class LemmaService implements LemmaFinderService {
    private final Thread parserThread;
    private final BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor;
    private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();
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
                    CollectedLemmas collectedLemmas = processPageEntityList(page);
                    syncObject.setPageEntity(page);
                    syncObject.setCollectedLemmas(collectedLemmas);
                    lemmaIndexCoupleList.add(syncObject);
                }
            }
            queueForDataProcessor.add(lemmaIndexCoupleList);
        }
    }
    private CollectedLemmas processPageEntityList(PageEntity page) {
        if (page != null) {
            String text = page.getContent();
            Map<String, Integer> processedPage = lemmaFinder.collectLemmas(text);
            return new CollectedLemmas(page, processedPage);
        }
        return null;
    }
}
