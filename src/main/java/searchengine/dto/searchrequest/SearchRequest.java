package searchengine.dto.searchrequest;
import lombok.Value;

@Value
public class SearchRequest {
    String query;
    String site;
    int offset;
    int limit;

}
