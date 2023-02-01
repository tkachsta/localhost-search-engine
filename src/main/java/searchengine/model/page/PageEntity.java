package searchengine.model.page;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.site.SiteEntity;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "page", indexes = {@Index(name = "url_path", columnList = "path")})
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int page_id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;
    @Column(name = "path")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity that = (PageEntity) o;
        return page_id == that.page_id && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page_id, path);
    }




//    @ManyToMany(cascade = CascadeType.ALL)
//    @JoinTable(
//            name = "index_table",
//            joinColumns = {@JoinColumn(name = "page_id")},
//            inverseJoinColumns = {@JoinColumn(name = "lemma_id")}
//    )
//    Set<LemmaEntity> lemmas = new HashSet<>();



}
