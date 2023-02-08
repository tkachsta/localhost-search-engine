package searchengine.indexer;
import lombok.Getter;

@Getter
public class IndexerType {
    private final IndexerMode indexerMode;
    private final int pageBatchSize;
    public IndexerType(IndexerMode indexerMode, int pageBatchSize) {
        this.indexerMode = indexerMode;
        this.pageBatchSize = pageBatchSize;
    }
}
