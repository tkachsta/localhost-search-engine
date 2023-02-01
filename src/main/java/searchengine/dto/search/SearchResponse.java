package searchengine.dto.search;
import java.util.List;
import lombok.Data;

@Data
public class SearchResponse {

    private boolean result;
    private int count;
    private List<DetailedSearchItem> data;

}
