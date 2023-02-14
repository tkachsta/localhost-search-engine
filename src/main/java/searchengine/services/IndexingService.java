package searchengine.services;

public interface IndexingService {
    boolean startMultipleSitesRecursiveIndexing();
    boolean startSinglePageIndexing(String url);
    boolean terminateIndexing();
}
