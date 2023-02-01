package searchengine.model.index;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import javax.persistence.*;


@Getter
@Setter
@Entity
@Table(name = "`index`")
public class IndexEntity {
    @EmbeddedId
    private IndexKey index_id;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private PageEntity page;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private LemmaEntity lemma;
    @Column(name = "`rank`", precision = 3, scale = 2)
    private float rank;

}
