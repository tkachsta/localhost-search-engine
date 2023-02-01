package searchengine.model.lemma;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.site.SiteEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository <LemmaEntity, Integer> {

    @Query("SELECT COUNT(*) FROM LemmaEntity")
    int findCount();
    @Query("SELECT COUNT(*) FROM LemmaEntity l WHERE l.site = :site")
    int findCountBySite(@Param("site") SiteEntity site);
    @Query("SELECT COUNT(DISTINCT site) FROM LemmaEntity")
    int findUniqNumberOfSites();
    @Query("SELECT SUM(frequency) FROM LemmaEntity")
    int totalSumOfFrequency();
    @Query("SELECT SUM(l.frequency) FROM LemmaEntity l WHERE l.lemma = :lemma")
    Optional<Integer> sumOfLemmaFrequency(@Param("lemma") String lemma);
    @Modifying
    @Query("DELETE FROM LemmaEntity l WHERE l.site = :site")
    void removeAllBySite(@Param("site") SiteEntity site);





}
