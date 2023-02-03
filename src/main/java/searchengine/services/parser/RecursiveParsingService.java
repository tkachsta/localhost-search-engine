package searchengine.services.parser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.page.PageEntity;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Service
public class RecursiveParsingService implements ParserService {
    private final SiteRepository siteRepository;
    private final SiteEntity siteEntity;
    private final Site site;
    private final IndexRatioModel ratioModel;
    private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;
    private final Set<String> redis = Collections.synchronizedSet(new HashSet<>());

    public RecursiveParsingService(SiteRepository siteRepository,
                                   Site site,
                                   IndexRatioModel ratioModel,
                                   BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService) {
        this.siteRepository = siteRepository;
        this.pageEntityQueueForLemmaService = pageEntityQueueForLemmaService;
        this.site = site;
        this.ratioModel = ratioModel;
        this.siteEntity = siteEntity();

    }
    @Bean("parserThread")
    public Thread parserThread() {
        return Thread.currentThread();
    }
    @Bean("siteEntity")
    private SiteEntity siteEntity() {
        return siteRepository.findByUrl(site.getUrl());
    }
    @Override
    public void runParsing() {

        ForkJoinPool fjp = new ForkJoinPool();
        ForkJoinTask<?> task = new RecursiveParsing(
                this.site.getUrl(), this.siteEntity, this.ratioModel,
                this.pageEntityQueueForLemmaService, fjp, redis);
        fjp.invoke(task);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void terminateParsing() {
        RecursiveParsing.terminateFjp();
    }
    public static void activateParsing() {
        RecursiveParsing.activateFjp();
    }
    private static class RecursiveParsing extends RecursiveAction {
        private static volatile boolean fjpTermination = true;
        private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;
        private final ForkJoinPool fjp;
        private final String node;
        private final SiteEntity site;
        private final IndexRatioModel ratioModel;
        private final Set<String> redis;
        public RecursiveParsing(String node,
                                SiteEntity site,
                                IndexRatioModel ratioModel,
                                BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService,
                                ForkJoinPool fjp,
                                Set<String> redis) {
            this.node = node;
            this.site = site;
            this.ratioModel = ratioModel;
            this.fjp = fjp;
            this.pageEntityQueueForLemmaService = pageEntityQueueForLemmaService;
            this.redis = redis;
        }

        @Override
        protected void compute() {
            ratioModel.incrementCreatedTasks();
            List<RecursiveParsing> taskList = new ArrayList<>();
            try {
                Connection.Response nodeResponse = getPageResponse(node);
                if (nodeResponse != null) {
                    sendToPageRepositoryQueue(getChildrenNodes(nodeResponse), taskList);
                    ForkJoinTask.invokeAll(taskList);
                    for (RecursiveParsing action : taskList) {
                        action.join();
                    }
                }
                if (!fjpTermination) {
                    fjp.shutdownNow();
                }
                ratioModel.incrementCompletedTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public static void terminateFjp() {
            fjpTermination = false;
        }
        public static void activateFjp() {
            fjpTermination = true;
        }
        private void sendToPageRepositoryQueue(Set<String> nodes, List<RecursiveParsing> taskList) {
            Set<String> queries = new HashSet<>(nodes);
            for (String node : nodes) {
                if (redis.contains(node)) {
                    queries.remove(node);
                }
            }
            List<PageEntity> multipleEntries = new ArrayList<>();
            parseAndAddToRecursion(queries, multipleEntries, taskList);
            pageEntityQueueForLemmaService.add(multipleEntries);
        }
        private void parseAndAddToRecursion (Set<String> nodes, List<PageEntity> multipleEntries,
                                             List<RecursiveParsing> taskList) {
            for (String child : nodes) {
                Connection.Response childResponse = getPageResponse(child);
                if (childResponse != null && nodeIsNotDuplicate(child)) {
                    multipleEntries.add(collectInformationOfPage(childResponse));
                    taskList.add(new RecursiveParsing(child, site, ratioModel,
                            pageEntityQueueForLemmaService, fjp, redis));
                }
            }
        }
        private Set<String> getChildrenNodes(Connection.Response response) throws Exception {
            Document doc;
            Set<String> childrenNodes = new HashSet<>();
            try {
                doc = response.parse();
            } catch (IOException e) {
                e.printStackTrace();
                throw new Exception();
            }
            Elements nodes = doc.select("a");
            nodes.forEach(n -> {
                String page = n.attr("abs:href");
                if (nodeIsCleanForProcessing(page)) {
                    childrenNodes.add(page);
                }
            });
            return childrenNodes;
        }
        private PageEntity collectInformationOfPage(Connection.Response response) {
            PageEntity page = new PageEntity();
            page.setPath(String.valueOf(response.url()).replace(site.getUrl(), ""));
            page.setSite(site);
            page.setCode(response.statusCode());
            page.setContent(String.valueOf(Jsoup.parse(response.body())));
            return page;
        }
        private Connection.Response getPageResponse(String url)  {
            if (url.startsWith(site.getUrl()) &&
                    nodeIsCleanForProcessing(url)) {
                try {
                    return  Jsoup
                            .connect(url)
                            .followRedirects(false)
                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; " +
                                    "rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com")
                            .timeout(20000)
                            .execute();
                } catch (IOException e) {
                    redis.add(url);
                    e.printStackTrace();
                }
            }
            return null;
        }
        private boolean nodeIsCleanForProcessing (String url) {
            return  !url.endsWith("jpg") &&
                    !url.endsWith("pdf") &&
                    !url.endsWith("jpeg") &&
                    !url.endsWith("JPG") &&
                    !url.endsWith("png") &&
                    !url.endsWith("PNG") &&
                    !url.endsWith("pptx") &&
                    !url.endsWith("docx") &&
                    !url.endsWith("PDF") &&
                    !url.endsWith("rar") &&
                    !url.endsWith("avi") &&
                    !url.endsWith("XLSX") &&
                    !url.endsWith("pdf.sig") &&
                    !url.endsWith("zip") &&
                    !url.endsWith("mp4") &&
                    !url.endsWith("doc") &&
                    !url.endsWith("ppt") &&
                    !url.endsWith("MP4") &&
                    !url.endsWith("xlsx") &&
                    !url.contains("#") &&
                    !url.contains("///") &&
                    !url.contains("goout") &&
                    !url.endsWith("xls") &&
                    !url.endsWith("rtf") &&
                    !url.endsWith("dot");
        }
        private boolean nodeIsNotDuplicate (String node) {
            if (redis.contains(node)) {
                return false;
            }
            redis.add(node);
            return true;
        }


    }

}




