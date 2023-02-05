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
import searchengine.dataprocessing.DataProcessor;
import searchengine.lemmafinder.LemmaFinderService;
import searchengine.lemmafinder.LemmaService;
import searchengine.parser.IndexRatioModel;
import searchengine.parser.ParserService;
import searchengine.parser.ParserUtil;
import searchengine.queue.LemmaRepositoryQueueService;
import searchengine.queue.PageRepositoryQueueService;
import searchengine.queue.StorageQueue;
import java.util.List;

public class IndexingUtil implements Runnable {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Site site;
    private final SiteEntity siteEntity;
    private final PageEntity pageEntity;
    private final ParserType parserType;
    private final StorageQueue storageQueue;
    private final IndexRatioModel ratioModel;
    private final static StatisticsData statisticsData = new StatisticsData();
    private DetailedStatisticsItem siteStatistic;

    public IndexingUtil(SiteRepository siteRepository,
                        PageRepository pageRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository,
                        ParserType parserType,
                        Site site, SiteEntity siteEntity,
                        PageEntity pageEntity) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.parserType = parserType;
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
                parserType, siteStatistic);
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
                        lemmaFinderThread, siteEntity, siteStatistic,
                        storageQueue.getPageEntityQueueForPageRepository(),
                        storageQueue.getQueueForDataProcessor(), parserType);
        Thread dataProcessor = new Thread(dp);
        dataProcessor.start();
        return dataProcessor;
    }
    private Thread startPageRepositoryQueueService(Thread dataProcessingThread) {
        PageRepositoryQueueService pageRepositoryQueueService =
                new PageRepositoryQueueService(
                        pageRepository, dataProcessingThread, parserType,
                        storageQueue.getPageEntityQueueForPageRepository(),
                        storageQueue.getLemmasEntityQueueForLemmasRepository(), siteStatistic);
        Thread threadPageWriter = new Thread(pageRepositoryQueueService);
        threadPageWriter.start();
        return threadPageWriter;
    }
    private Thread startLemmaRepositoryQueueService(Thread pageWriterThread) {
        LemmaRepositoryQueueService qs =
                new LemmaRepositoryQueueService(
                        lemmaRepository, indexRepository, siteRepository,
                        siteEntity, pageWriterThread, ratioModel, siteStatistic,
                        storageQueue.getLemmasEntityQueueForLemmasRepository(), parserType);
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
            if (item.getUrl().equals(site.getUrl())) {
                item.resetStatistics();
                siteStatistic = item;
            }
        }
        if (siteStatistic == null && parserType != ParserType.SINGLEPAGE) {
            siteStatistic =  new DetailedStatisticsItem();
            IndexingUtil.getStatisticsData().getDetailed().add(siteStatistic);
        }
    }

}
