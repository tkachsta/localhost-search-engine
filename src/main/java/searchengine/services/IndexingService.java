package searchengine.services;

import searchengine.indexer.IndexingRunningStatus;
import searchengine.parser.RecursiveParsingService;
import searchengine.services.impl.IndexingServiceImpl;

public interface IndexingService {

    boolean startMultipleSitesRecursiveIndexing();
    boolean startSinglePageIndexing(String url);
    static boolean terminateIndexing() {
        if (IndexingServiceImpl.getRunningStatus() == IndexingRunningStatus.RUNNING) {
            RecursiveParsingService.terminateParsing();
            return true;
        }
        return true;
    }


}
