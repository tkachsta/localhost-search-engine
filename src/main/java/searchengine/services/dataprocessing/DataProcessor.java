package searchengine.services.dataprocessing;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.site.SiteRepository;
import searchengine.services.dto.CollectedLemmas;
import searchengine.services.dto.Index;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexKey;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;
import searchengine.services.dto.LemmaIndexCouple;
import searchengine.services.ini.ParserType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@Transactional
public class DataProcessor implements Runnable {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
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
                         ParserType parserType,
                         LemmaRepository lemmaRepository,
                         SiteRepository siteRepository) {
        this.parserThread = parserThread;
        this.siteEntity = siteEntity;
        this.pageEntityQueueForPageRepository = pageEntityQueueForPageRepository;
        this.queueForDataProcessor = queueForDataProcessor;
        this.lemmaRepository = lemmaRepository;
        this.parserType = parserType;
        this.siteRepository = siteRepository;
        this.siteStatistic = siteStatistic;
        this.redisLemmas = new HashMap<>();
    }
    @Override
    public void run() {
        if (parserType == ParserType.MULTIPLESITES || parserType == ParserType.SINGLESITE) {
            processingMultiplePages();
        } else if (parserType == ParserType.SINGLEPAGE) {
            processingSinglePage();
        }
    }
    private void processingMultiplePages() {
        while (parserThread.isAlive() || !queueForDataProcessor.isEmpty()) {
            List<LemmaIndexCouple> pageLemmaIndexCouple = queueForDataProcessor.poll();
            if (pageLemmaIndexCouple == null) {
                continue;
            }
            try {
                multiplePageProcessor(pageLemmaIndexCouple, "multi");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            siteStatistic.setLemmas(redisLemmas.size());
        }
    }
    private void processingSinglePage() {
        while (parserThread.isAlive() || !queueForDataProcessor.isEmpty()) {
            List<LemmaIndexCouple> pageLemmaIndexCouple = queueForDataProcessor.poll();
            if (pageLemmaIndexCouple == null) {
                continue;
            }
            try {
                multiplePageProcessor(pageLemmaIndexCouple, "single");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void multiplePageProcessor(List<LemmaIndexCouple> pageLemmaIndexCouple, String option)
            throws InterruptedException {
        for (LemmaIndexCouple couple : pageLemmaIndexCouple) {
            List<Index> indexes = new ArrayList<>();
            List<LemmaEntity> lemmaEntityList = new ArrayList<>();
            singlePageProcessor(couple, lemmaEntityList, indexes, option);
        }
    }

    private void singlePageProcessor(LemmaIndexCouple pageLemmaIndexCouple,
                                     List<LemmaEntity> lemmaEntityList,
                                     List<Index> indexes,
                                     String option) throws InterruptedException {
        CollectedLemmas lemmas = pageLemmaIndexCouple.getCollectedLemmas();
        PageEntity pageEntity = lemmas.getPageEntity();
        Map<String, Integer> collectedLemmas = lemmas.getCollectedLemmas();
        processLemmaMap(collectedLemmas, lemmaEntityList, pageEntity, indexes, option);

        List<IndexEntity> indexesForRepository = prepareIndexesForRepository(indexes);
        if (lemmaEntityList.size() != 0) {
            pageLemmaIndexCouple.setLemmaEntityList(lemmaEntityList);
            pageLemmaIndexCouple.setIndexEntityList(indexesForRepository);
            pageEntityQueueForPageRepository.put(pageLemmaIndexCouple);
        };
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
                lemmaEntity.setLemma_id(lemmaEntity.getLemma_id());
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
    private void setupRedisLemmas() {
        Session session = HibernateUtil.buildSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<LemmaEntity> le = cb.createQuery(LemmaEntity.class);
        Root<LemmaEntity> root = le.from(LemmaEntity.class);
        root.fetch("site", JoinType.INNER);
        le.where(cb.equal(root.get("site").get("site_id"), siteEntity.getSite_id()));
        Query<LemmaEntity> query = session.createQuery(le);
        List<LemmaEntity> results = query.getResultList();
        System.out.println("Размер" + results.size());
        results.forEach(lemmaEntity -> {
            redisLemmas.put(lemmaEntity.getLemma(), lemmaEntity);
        });
    }
}
