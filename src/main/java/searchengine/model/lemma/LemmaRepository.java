package searchengine.model.lemma;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.site.SiteEntity;
import java.util.Optional;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository <LemmaEntity, Integer> {
    int countAllBy();
    int countAllBySite(SiteEntity site);
    @Modifying
    void deleteAllBySite(SiteEntity site);
    @Query("SELECT COUNT(DISTINCT site) FROM LemmaEntity")
    int findUniqNumberOfSites();
    @Query("SELECT SUM(frequency) FROM LemmaEntity")
    int totalSumOfFrequency();
    @Query("SELECT SUM(l.frequency) FROM LemmaEntity l WHERE l.lemma = :lemma")
    Optional<Integer> sumOfLemmaFrequency(@Param("lemma") String lemma);






}
