package searchengine.model.page;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.site.SiteEntity;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<PageEntity, Integer> {
    int countAllBy();
    int countAllBySiteIn(Collection<SiteEntity> siteEntities);
    int countAllBySite(SiteEntity siteEntity);
    int countByPath(String path);
    List<PageEntity> findAllByPathIn(Collection<String> names);
    List<PageEntity> findAllBySite(SiteEntity siteEntity);
    PageEntity findByPath(String path);
    @Modifying
    void deleteByPath(String path);
    @Modifying
    void deleteAllBySite(SiteEntity site);
    @Query("SELECT MAX(page_id) FROM PageEntity")
    int findMaxId();
    @Query("SELECT COUNT(DISTINCT site) FROM PageEntity")
    int findUniqNumberOfSites();


}


