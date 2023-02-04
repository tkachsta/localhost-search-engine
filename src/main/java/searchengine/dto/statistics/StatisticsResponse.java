package searchengine.dto.statistics;
import lombok.Value;

@Value
public class StatisticsResponse {
    boolean result;
    StatisticsData statistics;
}
