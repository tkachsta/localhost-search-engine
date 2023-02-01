package searchengine.services.parser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class SinglePageParsing implements ParserService {
    private final PageEntity pageEntity;
    private final SiteEntity siteEntity;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService;

    public SinglePageParsing(PageEntity pageEntity,
                             SiteEntity siteEntity,
                             LemmaRepository lemmaRepository,
                             IndexRepository indexRepository,
                             PageRepository pageRepository,
                             BlockingQueue<List<PageEntity>> pageEntityQueueForLemmaService) {
        this.pageEntity = pageEntity;
        this.siteEntity = siteEntity;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.pageEntityQueueForLemmaService = pageEntityQueueForLemmaService;
    }
    @Override
    public void runParsing() {
        String url = siteEntity.getUrl() + pageEntity.getPath();
        cleanIndexAndLemmaTables();
        try {
            createTaskForIndexing(url);
            Thread.sleep(2000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    private void createTaskForIndexing(String url) throws Exception {
        Connection.Response response = getPageResponse(url);
        PageEntity pageEntity = collectInformationOfPage(response, url);
        List<PageEntity> pageEntityList = new ArrayList<>();
        pageEntityList.add(pageEntity);
        pageEntityQueueForLemmaService.add(pageEntityList);
    }
    private Connection.Response getPageResponse(String url) throws Exception {
        if (nodeIsCleanForProcessing(url)) {
            return Jsoup
                    .connect(url)
                    .followRedirects(false)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; " +
                            "rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(20000)
                    .execute();
        }
        return null;
    }
    private PageEntity collectInformationOfPage(Connection.Response response, String url) {
        PageEntity page = new PageEntity();
        page.setPath(url.replace(siteEntity.getUrl(), ""));
        page.setSite(siteEntity);
        page.setCode(response.statusCode());
        page.setContent(Jsoup.parse(response.body()).text());
        return page;
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
                !url.contains("goout");
    }
    private void cleanIndexAndLemmaTables() {
        List<IndexEntity> indexEntityList = indexRepository.selectByPageId(pageEntity);
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        indexEntityList.forEach(indexEntity -> {
            LemmaEntity lemmaEntity = indexEntity.getLemma();
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() - (int) indexEntity.getRank());
            lemmaEntityList.add(lemmaEntity);
        });
        indexRepository.removeAllByPage(pageEntity);
        lemmaRepository.saveAll(lemmaEntityList);
        pageRepository.removePageById(pageEntity.getPage_id());
    }
}
