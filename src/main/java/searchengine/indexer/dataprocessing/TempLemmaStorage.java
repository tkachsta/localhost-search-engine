package searchengine.indexer.dataprocessing;
import org.hibernate.Session;
import org.hibernate.query.Query;
import searchengine.indexer.IndexerMode;
import searchengine.indexer.IndexerType;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.site.SiteEntity;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public record TempLemmaStorage(IndexerType indexerType, SiteEntity siteEntity) {
    public Map<String, LemmaEntity> getTempStorage() {
        if (indexerType.getIndexerMode() == IndexerMode.RECURSIVE) {
            return new HashMap<>();
        }
        return setupRedisLemmas();
    }
    private Map<String, LemmaEntity> setupRedisLemmas() {
        Map<String, LemmaEntity> tempStorage = new HashMap<>();
        Session session = HibernateUtil.buildSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<LemmaEntity> le = cb.createQuery(LemmaEntity.class);
        Root<LemmaEntity> root = le.from(LemmaEntity.class);
        root.fetch("site", JoinType.INNER);
        le.where(cb.equal(root.get("site").get("site_id"), siteEntity.getSite_id()));
        Query<LemmaEntity> query = session.createQuery(le);
        List<LemmaEntity> results = query.getResultList();
        results.forEach(lemmaEntity -> tempStorage.put(lemmaEntity.getLemma(), lemmaEntity));
        session.close();
        return tempStorage;
    }
}
