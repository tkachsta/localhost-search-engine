package searchengine.services.dataprocessing;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.services.dto.services.CollectedLemmas;
import searchengine.services.dto.services.Index;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexKey;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;
import searchengine.services.dto.services.LemmaIndexCouple;
import searchengine.services.indexer.ParserType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@Transactional
public class DataProcessor implements DataProcessingService {
    private final BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository;
    private final BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor;
    private final Map<String, LemmaEntity> redisLemmas;
    private final DetailedStatisticsItem siteStatistic;
    private final SiteEntity siteEntity;
    private final Thread parserThread;
    private final ParserType parserType;

    public DataProcessor(Thread parserThread,
                         SiteEntity siteEntity,
                         DetailedStatisticsItem siteStatistic,
                         BlockingQueue<LemmaIndexCouple> pageEntityQueueForPageRepository,
                         BlockingQueue<List<LemmaIndexCouple>> queueForDataProcessor,
                         ParserType parserType) {
        this.parserThread = parserThread;
        this.siteEntity = siteEntity;
        this.pageEntityQueueForPageRepository = pageEntityQueueForPageRepository;
        this.queueForDataProcessor = queueForDataProcessor;
        this.parserType = parserType;
        this.siteStatistic = siteStatistic;
        this.redisLemmas = new HashMap<>();
    }
    @Override
    public void run() {
        while (parserThread.isAlive() || !queueForDataProcessor.isEmpty()) {
            List<LemmaIndexCouple> pageLemmaIndexCouple = queueForDataProcessor.poll();
            if (pageLemmaIndexCouple == null) {
                continue;
            }
            if (parserType == ParserType.MULTIPLESITES || parserType == ParserType.SINGLESITE) {
                multiplePageProcessor(pageLemmaIndexCouple, "multi");
                siteStatistic.setLemmas(redisLemmas.size());
            } else if (parserType == ParserType.SINGLEPAGE) {
                multiplePageProcessor(pageLemmaIndexCouple, "single");
            }
        }
    }
    private void multiplePageProcessor(List<LemmaIndexCouple> pageLemmaIndexCouple, String option) {
        for (LemmaIndexCouple couple : pageLemmaIndexCouple) {
            List<Index> indexes = new ArrayList<>();
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            singlePageProcessor(couple, lemmaEntityList, indexes, option);
        }
    }
    private void singlePageProcessor(LemmaIndexCouple pageLemmaIndexCouple,
                                     List<LemmaEntity> lemmaEntityList,
                                     List<Index> indexes,
                                     String option){
        CollectedLemmas lemmas = pageLemmaIndexCouple.getCollectedLemmas();
        PageEntity pageEntity = lemmas.getPageEntity();
        Map<String, Integer> collectedLemmas = lemmas.getCollectedLemmas();
        processLemmaMap(collectedLemmas, lemmaEntityList, pageEntity, indexes, option);

        List<IndexEntity> indexesForRepository = prepareIndexesForRepository(indexes);
        if (lemmaEntityList.size() != 0) {
            pageLemmaIndexCouple.setLemmaEntityList(lemmaEntityList);
            pageLemmaIndexCouple.setIndexEntityList(indexesForRepository);
            try {
                pageEntityQueueForPageRepository.put(pageLemmaIndexCouple);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void processLemmaMap(Map<String, Integer> collectedLemmas,
                                 List<LemmaEntity> lemmaEntityList,
                                 PageEntity pageEntity,
                                 List<Index> indexes,
                                 String option) {
        if (option.contains("multi")) {
            multiplePageCollectedLemmasProcessing(collectedLemmas,
                    lemmaEntityList,pageEntity, indexes);
        } else if (option.contains("single")) {
            singlePageCollectedLemmasProcessing(collectedLemmas,
                    lemmaEntityList,pageEntity, indexes);
        }
    }
    private List<IndexEntity> prepareIndexesForRepository(List<Index> indexes) {
        List<IndexEntity> indexEntityList = new ArrayList<>();
        indexes.forEach(entry -> {
            IndexEntity index = new IndexEntity();
            IndexKey indexKey = new IndexKey(entry.pageEntity(), entry.lemmaEntity());
            index.setIndex_id(indexKey);
            index.setRank(entry.rating());
            indexEntityList.add(index);
        });
        return indexEntityList;
    }
    private void multiplePageCollectedLemmasProcessing (Map<String, Integer> collectedLemmas,
                                                        List<LemmaEntity> lemmaEntityList,
                                                        PageEntity pageEntity, List<Index> indexes) {
        siteEntity.setStatusTime(LocalDateTime.now());
        collectedLemmas.forEach((key, value) -> {
            LemmaEntity lemmaEntity;
            if (redisLemmas.containsKey(key)) {
                lemmaEntity = redisLemmas.get(key);
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            } else {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setLemma(key);
                lemmaEntity.setFrequency(1);
            }

            lemmaEntity.getSite().setStatusTime(LocalDateTime.now());
            redisLemmas.put(key, lemmaEntity);
            lemmaEntityList.add(lemmaEntity);
            Index index = new Index(pageEntity, lemmaEntity, value);
            indexes.add(index);
        });
    }

    private void singlePageCollectedLemmasProcessing (Map<String, Integer> collectedLemmas,
                                                      List<LemmaEntity> lemmaEntityList,
                                                      PageEntity pageEntity, List<Index> indexes) {
        setupRedisLemmas();
        collectedLemmas.forEach((key, value) -> {
            LemmaEntity lemmaEntity;
            if (redisLemmas.containsKey(key)) {
                lemmaEntity = redisLemmas.get(key);
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            } else {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setLemma(key);
                lemmaEntity.setFrequency(1);
            }
            System.out.println(lemmaEntity.getLemma());
            lemmaEntity.getSite().setStatusTime(LocalDateTime.now());
            redisLemmas.put(key, lemmaEntity);
            lemmaEntityList.add(lemmaEntity);
            Index index = new Index(pageEntity, lemmaEntity, value);
            indexes.add(index);
        });
    }
    private void setupRedisLemmas() {
        Session session = HibernateUtil.buildSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<LemmaEntity> le = cb.createQuery(LemmaEntity.class);
        Root<LemmaEntity> root = le.from(LemmaEntity.class);
        root.fetch("site", JoinType.INNER);
        le.where(cb.equal(root.get("site").get("site_id"), siteEntity.getSite_id()));
        Query<LemmaEntity> query = session.createQuery(le);
        List<LemmaEntity> results = query.getResultList();
        results.forEach(lemmaEntity -> redisLemmas.put(lemmaEntity.getLemma(), lemmaEntity));
        session.close();
    }
}
