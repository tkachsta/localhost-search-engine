package searchengine.dto.search;
import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private String site;
    private int offset;
    private int limit;

}
