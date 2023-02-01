package searchengine.model.index;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class IndexKey implements Serializable {
    @ManyToOne
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private PageEntity page;
    @ManyToOne
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private LemmaEntity lemma;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexKey indexKey = (IndexKey) o;
        return Objects.equals(page, indexKey.page) && Objects.equals(lemma, indexKey.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, lemma);
    }
}
