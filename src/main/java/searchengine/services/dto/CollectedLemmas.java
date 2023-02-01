package searchengine.services.dto;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.page.PageEntity;
import java.util.Map;

@Setter
@Getter
public class CollectedLemmas {
    private PageEntity pageEntity;
    private Map<String, Integer> collectedLemmas;

}
