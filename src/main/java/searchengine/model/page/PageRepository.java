package searchengine.model.page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.site.SiteEntity;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<PageEntity, Integer> {
    @Query("SELECT COUNT(*) FROM PageEntity")
    int findCount();
    @Query("SELECT COUNT(*) FROM PageEntity WHERE site IN :param")
    int findCountBySites(@Param("param") Collection<SiteEntity> siteEntities);
    @Query("SELECT COUNT(*) FROM PageEntity p WHERE p.site = :param")
    int findCountBySite(@Param("param") SiteEntity siteEntities);
    @Query("SELECT MAX(id) FROM PageEntity")
    int findMaxId();
    @Query("SELECT COUNT(DISTINCT site) FROM PageEntity")
    int findUniqNumberOfSites();
    @Query("SELECT COUNT(*) FROM PageEntity WHERE path = :path")
    int findPressenceOfPage(@Param("path") String path);
    @Query("SELECT p FROM PageEntity p WHERE p.path IN :param")
    List<PageEntity> multipleSelectByPath(@Param("param") Collection<String> names);
    @Query("SELECT p FROM PageEntity p WHERE p.site IN :site")
    List<PageEntity> findAllBySite(@Param("site") SiteEntity siteEntity);
    @Query("SELECT p FROM PageEntity p WHERE p.path = :path")
    PageEntity findPageByPath(@Param("path") String path);
    @Modifying
    @Query("DELETE FROM PageEntity p WHERE p.site = :site")
    void removeAllBySite(@Param("site") SiteEntity site);
    @Modifying
    @Query("DELETE FROM PageEntity p WHERE p.page_id = :id")
    void removePageById(@Param("id") int id);
}


