package searchengine.model.site;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
@Transactional
public interface SiteRepository extends CrudRepository<SiteEntity, Integer> {
    @Query("SELECT COUNT(*) FROM SiteEntity")
    int findCount();
    @Query("SELECT s FROM SiteEntity s WHERE s.status = :status")
    Iterable<SiteEntity> findByStatus(@Param("status") IndexingStatus status);
    @Query("SELECT s FROM SiteEntity s")
    List<SiteEntity> findAllSites();
    @Query("SELECT s FROM SiteEntity s WHERE s.url = :url")
    SiteEntity findByUrl(@Param("url") String url);
    @Modifying
    @Query("DELETE FROM SiteEntity s WHERE s.url = :url")
    void removeAllByUrl(@Param("url") String url);

}
