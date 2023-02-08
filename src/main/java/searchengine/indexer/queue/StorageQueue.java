package searchengine.indexer.queue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import searchengine.model.page.PageEntity;
import searchengine.dto.indexing.LemmaIndexCouple;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
public class StorageQueue {
    private final BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> lemmasEntityQueueForLemmasRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor;
    private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;
    public StorageQueue() {
        this.pageEntityQueueForPageRepository =
                new ArrayBlockingQueue<>(20000);
        this.lemmasEntityQueueForLemmasRepository =
                new ArrayBlockingQueue<>(20000);
        this.pageEntityQueueForLemmaService =
                new ArrayBlockingQueue<>(20000);
        this.queueForDataProcessor =
                new ArrayBlockingQueue<>(20000);
    }


    @Bean("pageEntityQueueForPageRepository")
    public BlockingQueue<LemmaIndexCouple> getPageEntityQueueForPageRepository() {
        return pageEntityQueueForPageRepository;
    }
    @Bean("pageEntityQueueForLemmaService")
    public BlockingQueue<List<PageEntity>> getPageEntityQueueForLemmaService() {
        return pageEntityQueueForLemmaService;
    }
    @Bean("lemmasEntityQueueForLemmasRepository")
    public BlockingQueue<List<LemmaIndexCouple>> getLemmasEntityQueueForLemmasRepository() {
        return lemmasEntityQueueForLemmasRepository;
    }
    @Bean("queueForDataProcessor")
    public BlockingQueue<List<LemmaIndexCouple>> getQueueForDataProcessor() {
        return queueForDataProcessor;
    }

}
