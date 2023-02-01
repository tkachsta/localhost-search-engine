package searchengine.dto.statistics;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@Getter
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;
    public void incrementPages(int number) {
        pages += number;
    }
    public void resetStatistics() {
        this.statusTime = System.currentTimeMillis();
        this.error = "";
        this.status = "INDEXING";
        this.pages = 0;
        this.lemmas = 0;
    }
}
