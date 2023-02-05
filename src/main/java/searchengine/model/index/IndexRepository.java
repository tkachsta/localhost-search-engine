package searchengine.model.index;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends CrudRepository <IndexEntity, Integer> {

    List<IndexEntity> findAllByPage(PageEntity pageEntity);
    Collection<IndexEntity> findIndexEntitiesByLemma_LemmaAndLemma_SiteIn(
            String lemma, Collection<SiteEntity> siteEntities);
    Collection<IndexEntity> findAllByLemma_LemmaAndLemma_SiteInAndPageIn(
            String lemma, Collection<SiteEntity> siteEntities, Collection<PageEntity> pages);
    @Modifying
    @Query("DELETE FROM IndexEntity i WHERE i.page IN :param")
    void removeAllByPage(@Param("param") PageEntity pageEntity);
    @Modifying
    @Query("DELETE FROM IndexEntity i WHERE i.page IN :param")
    void removeAllByPages(@Param("param") Collection<PageEntity> pageEntityList);

}
