package searchengine.indexer;
import searchengine.config.Site;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.indexer.dataprocessing.DataProcessor;
import searchengine.indexer.lemmafinder.LemmaFinderService;
import searchengine.indexer.lemmafinder.LemmaService;
import searchengine.indexer.parser.IndexRatioModel;
import searchengine.indexer.parser.ParserService;
import searchengine.indexer.parser.ParserUtil;
import searchengine.indexer.queue.LemmaRepositoryQueueService;
import searchengine.indexer.queue.PageRepositoryQueueService;
import searchengine.indexer.queue.StorageQueue;
import java.util.List;

public class IndexingUtil implements Runnable {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Site site;
    private final SiteEntity siteEntity;
    private final PageEntity pageEntity;
    private final IndexerType indexerType;
    private final StorageQueue storageQueue;
    private final IndexRatioModel ratioModel;
    private final static StatisticsData statisticsData = new StatisticsData();
    private DetailedStatisticsItem siteStatistic;

    public IndexingUtil(SiteRepository siteRepository,
                        PageRepository pageRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository,
                        IndexerType indexerType,
                        Site site, SiteEntity siteEntity,
                        PageEntity pageEntity) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.indexerType = indexerType;
        this.site = site;
        this.siteEntity = siteEntity;
        this.pageEntity = pageEntity;
        this.storageQueue = new StorageQueue();
        this.ratioModel = new IndexRatioModel();
        synchronization();
    }

    @Override
    public void run() {
        Thread parserThread = startParserService();
        Thread lemmaFinderThread = startLemmaFinderService(parserThread);
        Thread dataProcessingThread = startDataProcessingService(lemmaFinderThread);
        Thread pageWriterThread = startPageRepositoryQueueService(dataProcessingThread);
        Thread lemmaIndexWriterThread = startLemmaRepositoryQueueService(pageWriterThread);
        try {
            lemmaIndexWriterThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private Thread startParserService() {
        ParserUtil parserUtil = new ParserUtil(
                siteRepository, pageRepository, lemmaRepository, indexRepository,
                siteEntity, pageEntity, site, ratioModel, storageQueue.getPageEntityQueueForLemmaService(),
                indexerType, siteStatistic);
        ParserService parser = parserUtil.createParsingInstance();
        Thread threadParser = new Thread(parser::runParsing);
        threadParser.start();
        return threadParser;
    }
    private Thread startLemmaFinderService(Thread parserThread) {
        LemmaFinderService lfs =
                new LemmaService(
                        parserThread,
                        storageQueue.getQueueForDataProcessor(),
                        storageQueue.getPageEntityQueueForLemmaService());
        Thread threadLemmaFinder = new Thread(lfs::runLemmatization);
        threadLemmaFinder.start();
        return threadLemmaFinder;
    }
    private Thread startDataProcessingService(Thread lemmaFinderThread) {
        DataProcessor dp = new DataProcessor(
                        lemmaFinderThread, siteEntity,
                        storageQueue.getPageEntityQueueForPageRepository(),
                        storageQueue.getQueueForDataProcessor(), indexerType);
        Thread dataProcessor = new Thread(dp);
        dataProcessor.start();
        return dataProcessor;
    }
    private Thread startPageRepositoryQueueService(Thread dataProcessingThread) {
        PageRepositoryQueueService pageRepositoryQueueService =
                new PageRepositoryQueueService(
                        pageRepository, dataProcessingThread, indexerType,
                        storageQueue.getPageEntityQueueForPageRepository(),
                        storageQueue.getLemmasEntityQueueForLemmasRepository());
        Thread threadPageWriter = new Thread(pageRepositoryQueueService);
        threadPageWriter.start();
        return threadPageWriter;
    }
    private Thread startLemmaRepositoryQueueService(Thread pageWriterThread) {
        LemmaRepositoryQueueService qs =
                new LemmaRepositoryQueueService(
                        lemmaRepository, indexRepository, siteRepository,
                        siteEntity, pageWriterThread, ratioModel, siteStatistic,
                        storageQueue.getLemmasEntityQueueForLemmasRepository(), indexerType);
        Thread LQS = new Thread(qs);
        LQS.start();
        return LQS;
    }
    public static StatisticsData getStatisticsData() {
        return statisticsData;
    }
    private void synchronization() {
        List<DetailedStatisticsItem> statisticsItems = IndexingUtil.getStatisticsData().getDetailed();
        for (DetailedStatisticsItem item : statisticsItems) {
            if (item.getUrl() == null) {
                continue;
            }
            if (item.getUrl().equals(site.getUrl())) {
                item.resetStatistics();
                siteStatistic = item;
            }
        }
        if (siteStatistic == null && indexerType.getIndexerMode() != IndexerMode.SINGLEPAGE) {
            siteStatistic =  new DetailedStatisticsItem();
            IndexingUtil.getStatisticsData().getDetailed().add(siteStatistic);
        }
    }

}
