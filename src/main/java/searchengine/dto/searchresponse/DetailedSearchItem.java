package searchengine.dto.searchresponse;
import lombok.Data;
@Data
public class DetailedSearchItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

}
