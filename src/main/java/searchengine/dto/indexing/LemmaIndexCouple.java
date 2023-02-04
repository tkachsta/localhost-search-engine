package searchengine.dto.indexing;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.index.IndexEntity;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;

import java.util.List;
@Getter
@Setter
public class LemmaIndexCouple {
    private List<LemmaEntity> lemmaEntityList;
    private List<IndexEntity> indexEntityList;
    private CollectedLemmas collectedLemmas;
    private PageEntity pageEntity;
}


