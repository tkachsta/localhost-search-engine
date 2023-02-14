package searchengine.indexer;


import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class IndexerType {
    private IndexerMode indexerMode;
    private int pageBatchSize;

}
