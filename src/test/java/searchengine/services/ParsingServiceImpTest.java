package searchengine.services;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.services.indexer.IndexingService;
import searchengine.services.parser.RecursiveParsingService;

import java.util.List;

@SpringBootTest
@DisplayName("Запись данных парсинга сайта в БД")
@TestClassOrder(ClassOrderer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(5)
class ParsingServiceImpTest {
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    SitesList sitesList;
    @Autowired
    RecursiveParsingService recursiveParsingService;
    @Autowired
    Site site;

    @BeforeEach
    public void setUp () {
         sitesList.clearSitesList();
    }

    @Test
    @Order(1)
    @DisplayName("Парсинг одного сайта")
    public void singleSiteParsing() {
        site.setUrl("http://school6.m-sk.ru");
        site.setName("http://school6.m-sk.ru");

        IndexingService ws = new IndexingService(
                siteRepository,
                pageRepository,
                lemmaRepository,
                indexRepository,
                sitesList);
        ws.startSingleSiteRecursiveIndexing(site);

    }
    @Test
    @Order(2)
    @DisplayName("Парсинг одной страницы")
    public void singlePageParsing()  {
        int id = 61;
        SiteEntity siteEntity = siteRepository.findByUrl("http://school6.m-sk.ru");
        PageEntity pageEntity = pageRepository.findById(id).get();
        IndexingService ws = new IndexingService(siteRepository,
                pageRepository,
                lemmaRepository,
                indexRepository,
                sitesList);
        String pageURL = siteEntity.getUrl() + pageEntity.getPath();
        ws.startSinglePageIndexing(pageURL);

    }
    @Test
    @Order(3)
    @DisplayName("Остановка парсинга")
    public void terminateIndexing()  {
        site.setUrl("http://playback.ru");
        site.setName("http://playback.ru");
        IndexingService ws = new IndexingService(
                siteRepository, pageRepository,
                lemmaRepository, indexRepository, sitesList);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                IndexingService.terminateIndexing();
            } catch (InterruptedException | JSONException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        ws.startSingleSiteRecursiveIndexing(site);
    }
    @Test
    @Order(4)
    @DisplayName("Парсинг нескольких сайтов")
    public void multipleSitesParsing() {
        sitesList.clearSitesList();

        Site site4 = new Site();
        site4.setUrl("http://school6.m-sk.ru");
        site4.setName("http://school6.m-sk.ru");
        sitesList.addSite(site4);

        Site site5 = new Site();
        site5.setUrl("http://playback.ru");
        site5.setName("http://playback.ru");
        sitesList.addSite(site5);

        IndexingService ws = new IndexingService(
                siteRepository,
                pageRepository,
                lemmaRepository,
                indexRepository,
                sitesList);
        ws.startMultipleSitesRecursiveIndexing();
    }
    @Test
    @Order(5)
    @DisplayName("Очистка странц, сайтов, лемм и индексов")
    public void cleanTable() {
        site.setUrl("https://stavmuseum.ru");


        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        List<PageEntity> pageEntityList = pageRepository.findAllBySite(siteEntity);
        indexRepository.removeAllByPages(pageEntityList);
        lemmaRepository.removeAllBySite(siteEntity);
        pageRepository.removeAllBySite(siteEntity);
        siteRepository.removeAllByUrl(site.getUrl());

    }





}