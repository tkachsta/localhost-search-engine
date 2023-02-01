package searchengine.dto.statistics;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
public class StatisticsData {
    private TotalStatistics total = new TotalStatistics();
    private List<DetailedStatisticsItem> detailed = new ArrayList<>();
}
