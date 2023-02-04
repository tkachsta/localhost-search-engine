package searchengine.dto.searchresponse;
import lombok.Value;

@Value
public class FalseResponse {
    boolean result;
    String error;
}
