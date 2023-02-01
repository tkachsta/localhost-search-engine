package searchengine.services.indexer;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.site.IndexingStatus;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.services.parser.RecursiveParsingService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IndexingService {
    private static IndexingRunningStatus runningStatus = IndexingRunningStatus.IDLE;
    private static JSONObject responseBody = new JSONObject();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;

    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository,
                           LemmaRepository lemmaRepository, IndexRepository indexRepository, SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sitesList = sitesList;
    }

    public void startMultipleSitesRecursiveIndexing() {
        runningStatus = IndexingRunningStatus.RUNNING;
        recursiveParsingResponse();
        RecursiveParsingService.activateParsing();
        List<Thread> threads = new ArrayList<>();
        for (Site site : sitesList.getSites()) {
            Thread thread = new Thread(() -> startSingleSiteRecursiveIndexing(site));
            thread.start();
            threads.add(thread);
        }
        for (Thread th : threads) {
            try {
                th.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void startSingleSiteRecursiveIndexing(Site site) {
        SiteEntity siteEntity = updateSiteAndPagesOnStart(site);
        IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                ParserType.SINGLESITE, site, siteEntity, new PageEntity());
        Thread siteIndexing = new Thread(indexingUtil);
        siteIndexing.start();
        try {
            siteIndexing.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startSinglePageIndexing(String url) {
        String domenRegex = "((?:http|https)://.+(?:ru|com))(/.+)";
        Pattern pattern = Pattern.compile(domenRegex);
        Matcher matcher = pattern.matcher(url);
        matcher.matches();
        String siteUrl = matcher.group(1).trim();
        String pageUrl = matcher.group(2).trim();
        SiteEntity siteEntity = siteRepository.findByUrl(siteUrl);
        PageEntity pageEntity = pageRepository.findPageByPath(pageUrl);
        if (validatePageForIndexing(siteUrl)) {
            IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                    ParserType.SINGLEPAGE, new Site(), siteEntity, pageEntity);
            Thread pageIndexing = new Thread(indexingUtil);
            pageIndexing.start();
            try {
                pageIndexing.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void terminateIndexing() throws JSONException {
        cleanResponseBody();
        if (runningStatus == IndexingRunningStatus.IDLE) {
            responseBody.put("result", false);
            responseBody.put("error", "Индексация не запущена");
        } else {
            responseBody.put("result", true);
            RecursiveParsingService.terminateParsing();
            runningStatus = IndexingRunningStatus.IDLE;
        }
    }

    private SiteEntity updateSiteAndPagesOnStart(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        List<PageEntity> pageEntityList = pageRepository.findAllBySite(siteEntity);
        indexRepository.removeAllByPages(pageEntityList);
        lemmaRepository.removeAllBySite(siteEntity);
        pageRepository.removeAllBySite(siteEntity);
        siteRepository.removeAllByUrl(site.getUrl());

        siteEntity = new SiteEntity();
        siteEntity.setStatus(IndexingStatus.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    private boolean validatePageForIndexing(String url) { // TODO create validation service
        for (Site site : sitesList.getSites()) {
            System.out.println(site.getUrl());
            if (site.getUrl().trim().compareTo(url) == 0) {
                try {
                    responseBody.put("result", true);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return true;
            } else {
                try {
                    responseBody.put("result", false);
                    responseBody.put("error", "Данная страница находится за пределами сайтов," +
                            "указанных в конфигурационном файле");
                    System.out.println("не содержит");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }

    public static void recursiveParsingResponse() {
        cleanResponseBody();
        JSONObject jo = new JSONObject();
        try {
            if (runningStatus == IndexingRunningStatus.IDLE) {
                jo.put("result", true);
            } else if (runningStatus == IndexingRunningStatus.RUNNING) {
                jo.put("result", false);
                jo.put("error", "Индексация уже запущена");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseBody = jo;
    }

    public static IndexingRunningStatus getRunningStatus() {
        return runningStatus;
    }

    public static JSONObject getResponseBody() {
        return responseBody;
    }
    private static void cleanResponseBody() {
        Iterator keys = responseBody.keys();
        try {
            while (keys.hasNext()) {
                responseBody.remove((String) responseBody.keys().next());
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }
}


