package searchengine.services.indexer;

import searchengine.services.parser.RecursiveParsingService;

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
