package searchengine.syncronization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.indexer.IndexingUtil;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class StatisticsSynchronization {
    private final StatisticsData statisticsData = IndexingUtil.getStatisticsData();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public void dataBaseSync() {
        if (statisticsData.getDetailed().isEmpty()) {
            List<SiteEntity> siteEntityList = siteRepository.findAllBy();
            siteEntityList.forEach(site -> {
                if (validateSiteForSynchronization(site)) {
                    DetailedStatisticsItem siteStatistic = new DetailedStatisticsItem();
                    siteStatistic.setUrl(site.getUrl());
                    siteStatistic.setName(site.getName());
                    statisticsData.getDetailed().add(siteStatistic);
                    siteStatistic.setStatus(site.getStatus().toString());
                    siteStatistic.setStatusTime(ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault()).toInstant().toEpochMilli());
                    siteStatistic.setError(site.getLastError());
                    siteStatistic.setLemmas(lemmaRepository.countAllBySite(site));
                    siteStatistic.setPages(pageRepository.countAllBySite(site));

                }
            });
        }
    }
    public boolean validateSiteForSynchronization(SiteEntity site) {
        List<DetailedStatisticsItem> statisticsItems = statisticsData.getDetailed();
        for (DetailedStatisticsItem item : statisticsItems) {
            if (item.getUrl().equals(site.getUrl())) {
                return false;
            }
        }
        return true;
    }

}
