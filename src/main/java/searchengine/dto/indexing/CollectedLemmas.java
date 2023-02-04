package searchengine.dto.indexing;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import searchengine.model.page.PageEntity;
import java.util.Map;

@Setter
@Getter
@Value
public class CollectedLemmas {
    PageEntity pageEntity;
    Map<String, Integer> collectedLemmas;

}
