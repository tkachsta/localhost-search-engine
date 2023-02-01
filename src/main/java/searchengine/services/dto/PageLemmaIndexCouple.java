package searchengine.services.dto;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.index.IndexEntity;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import java.util.List;

@Getter
@Setter
public class PageLemmaIndexCouple {

    private List<PageEntity> pageEntityList;
    private List<LemmaEntity> lemmaEntityList;
    private List<IndexEntity> indexEntityList;
    private List<CollectedLemmas> collectedLemmas;

}
