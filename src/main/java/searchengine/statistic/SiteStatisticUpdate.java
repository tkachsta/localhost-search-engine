package searchengine.statistic;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.indexer.IndexerMode;
import searchengine.indexer.IndexerType;
import searchengine.model.site.IndexingStatus;

public record SiteStatisticUpdate(
        IndexerType indexerType,
        DetailedStatisticsItem siteStatistic
) {
    public void updateCurrentTime() {
        if (indexerType.getIndexerMode() == IndexerMode.RECURSIVE) {
            siteStatistic.setStatusTime(System.currentTimeMillis());
        }
    }
    public void updateStatus(IndexingStatus indexingStatus) {
        if (indexerType.getIndexerMode() == IndexerMode.RECURSIVE) {
            siteStatistic.setStatus(indexingStatus.toString());
        }
    }
    public void updateLemmaCount(int lemmaCount) {
        if (indexerType.getIndexerMode() == IndexerMode.RECURSIVE) {
            siteStatistic.setLemmas(lemmaCount);
        }
    }
    public void updatePageCount(int pageCount) {
        if (indexerType.getIndexerMode() == IndexerMode.RECURSIVE) {
            siteStatistic.setPages(siteStatistic.getPages() + pageCount);
        }
    }

}
