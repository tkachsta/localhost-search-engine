package searchengine.model.index;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends CrudRepository <IndexEntity, Integer> {

    @Query("SELECT i FROM IndexEntity i WHERE i.page IN :param")
    List<IndexEntity> selectByPageId(@Param("param") PageEntity pageEntity);
    @Query("SELECT i FROM IndexEntity i WHERE i.lemma.lemma = :lemma AND i.lemma.site IN :sites")
    Collection<IndexEntity> selectIndexesByKey(@Param("lemma") String lemma,
                                               @Param("sites")Collection<SiteEntity> siteEntities);
    @Query("SELECT i FROM IndexEntity i WHERE i.lemma.lemma = :lemma AND i.page IN :pages AND i.lemma.site IN :sites")
    Collection<IndexEntity> selectIndexesByLemmaAndPage(@Param("lemma") String lemma,
                                                        @Param("pages") Collection<PageEntity> pages,
                                                        @Param("sites") Collection<SiteEntity> sites);

    @Modifying
    @Query("DELETE FROM IndexEntity i WHERE i.page IN :param")
    void removeAllByPage(@Param("param") PageEntity pageEntity);
    @Modifying
    @Query("DELETE FROM IndexEntity i WHERE i.page IN :param")
    void removeAllByPages(@Param("param") Collection<PageEntity> pageEntityList);

}
