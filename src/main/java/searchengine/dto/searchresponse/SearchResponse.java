package searchengine.dto.searchresponse;
import java.util.List;
import lombok.Data;

@Data
public class SearchResponse {

    private boolean result;
    private int count;
    private List<DetailedSearchItem> data;

}
