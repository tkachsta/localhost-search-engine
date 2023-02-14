package searchengine.services;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.services.impl.IndexingServiceImpl;
import searchengine.indexer.parser.RecursiveParsingService;

import java.util.List;
import java.util.Optional;

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
    @Autowired
    IndexingServiceImpl indexingService;

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

//        IndexingServiceImpl ws = new IndexingServiceImpl(
//                siteRepository,
//                pageRepository,
//                lemmaRepository,
//                indexRepository,
//                sitesList);
        indexingService.startSingleSiteRecursiveIndexing(site);

    }
    @Test
    @Order(2)
    @DisplayName("Парсинг одной страницы")
    public void singlePageParsing()  {
        int id = 61;
        SiteEntity siteEntity = siteRepository.findByUrl("http://school6.m-sk.ru");
        Optional<PageEntity> pageEntity = pageRepository.findById(id);
        if (pageEntity.isPresent()) {
            String pageURL = siteEntity.getUrl() + pageEntity.get().getPath();
            indexingService.startSinglePageIndexing(pageURL);
        }
    }
    @Test
    @Order(3)
    @DisplayName("Остановка парсинга")
    public void terminateIndexing()  {
//        site.setUrl("http://playback.ru");
//        site.setName("http://playback.ru");
//        IndexingServiceImpl ws = new IndexingServiceImpl(
//                siteRepository, pageRepository,
//                lemmaRepository, indexRepository, sitesList);
//        Thread thread = new Thread(() -> {
//            try {
//                Thread.sleep(5000);
//                IndexingServiceImpl.terminateIndexing();
//            } catch (InterruptedException | JSONException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        thread.start();
//        ws.startSingleSiteRecursiveIndexing(site);
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

        indexingService.startMultipleSitesRecursiveIndexing();
    }
    @Test
    @Order(5)
    @DisplayName("Очистка странц, сайтов, лемм и индексов")
    public void cleanTable() {
        site.setUrl("https://stavmuseum.ru");


        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        List<PageEntity> pageEntityList = pageRepository.findAllBySite(siteEntity);
        indexRepository.removeAllByPages(pageEntityList);
        lemmaRepository.deleteAllBySite(siteEntity);
        pageRepository.deleteAllBySite(siteEntity);
        siteRepository.deleteAllByUrl(site.getUrl());

    }





}