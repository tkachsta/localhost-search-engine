package searchengine.services.statistic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.services.ini.IndexingUtil;
import java.util.List;
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsData statisticsData = IndexingUtil.getStatisticsData();
        List<DetailedStatisticsItem> detailed = statisticsData.getDetailed();
        TotalStatistics total = statisticsData.getTotal();
        StatisticsResponse response = new StatisticsResponse();
        int pages = 0;
        int lemmas = 0;
        if (detailed != null) {
            for (DetailedStatisticsItem site : detailed) {
                pages = pages + site.getPages();
                lemmas = lemmas + site.getLemmas();
            }
            total.setSites(detailed.size());
            total.setPages(pages);
            total.setLemmas(lemmas);
            total.setIndexing(true);
            response.setStatistics(statisticsData);
            response.setResult(true);
        }
        return response;
    }
}
