package searchengine.model.lemma;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.site.SiteEntity;


@Getter
@Setter
@Entity
@Table (name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int lemma_id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;
    @Column (nullable = false)
    private String lemma;
    @Column
    private int frequency;
//    @OneToMany(mappedBy = "lemma")
//    private List<IndexEntity> indexes = new ArrayList<>();
//    @ManyToMany(mappedBy = "lemmas")
//    private Set<PageEntity> pages = new HashSet<>();



}
