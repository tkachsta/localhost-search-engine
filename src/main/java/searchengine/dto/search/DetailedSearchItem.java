package searchengine.dto.search;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class DetailedSearchItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

}
