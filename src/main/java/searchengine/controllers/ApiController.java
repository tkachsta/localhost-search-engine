package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteRepository;
import searchengine.services.indexer.IndexingRunningStatus;
import searchengine.services.indexer.IndexingService;
import searchengine.services.indexer.IndexingUtil;
import searchengine.services.search.SearchService;
import searchengine.services.statistic.StatisticsService;
import searchengine.services.synchronization.StatisticsSynchronization;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    @Autowired
    private final StatisticsService statisticsService;
    @Autowired
    private final SearchService searchService;
    @Autowired
    private final SitesList sitesList;

    public ApiController(StatisticsService statisticsService,
                         SearchService searchService,
                         PageRepository pageRepository,
                         SiteRepository siteRepository,
                         LemmaRepository lemmaRepository,
                         IndexRepository indexRepository,
                         SitesList sitesList) {
        this.statisticsService = statisticsService;
        this.searchService = searchService;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sitesList = sitesList;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        StatisticsResponse response = statisticsService.getStatistics();
        if (IndexingService.getRunningStatus() == IndexingRunningStatus.IDLE) {
            StatisticsSynchronization synchronization =
                    new StatisticsSynchronization(siteRepository, pageRepository, lemmaRepository);
            synchronization.run();
        }
        return ResponseEntity.ok(response);
    }
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam int offset,
                                                 @RequestParam int limit,
                                                 @RequestParam(required = false) String site) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setOffset(offset);
        searchRequest.setLimit(limit);
        searchRequest.setSite(site);
        return ResponseEntity.ok(searchService.getSearchResult(searchRequest));
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() throws InterruptedException {
        if (IndexingService.getRunningStatus() == IndexingRunningStatus.IDLE) {
            IndexingService.recursiveParsingResponse();
            ResponseEntity<String> response =
                    new ResponseEntity<>(IndexingService.getResponseBody().toString(), HttpStatus.OK);
            Thread thread = new Thread(() -> {
                IndexingService indexingService =
                        new IndexingService(siteRepository, pageRepository,
                                lemmaRepository, indexRepository, sitesList);
                indexingService.startMultipleSitesRecursiveIndexing();
            });
            thread.start();
            Thread.sleep(200);
            return response;
        }
        return new ResponseEntity<>(IndexingService.getResponseBody().toString(), HttpStatus.OK);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                IndexingService.terminateIndexing();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        Thread.sleep(300);
        return new ResponseEntity<>(IndexingService.getResponseBody().toString(), HttpStatus.OK);
    }
    @PostMapping(value = "/indexPage")
    public ResponseEntity startPageIndexing(@RequestParam String url) throws InterruptedException {
        Thread thread = new Thread(() -> {
            IndexingService indexingService = new IndexingService(siteRepository, pageRepository,
                    lemmaRepository, indexRepository, sitesList);
            indexingService.startSinglePageIndexing(url);
        });
        thread.start();
        Thread.sleep(200);
        return new ResponseEntity<>(IndexingService.getResponseBody().toString(), HttpStatus.OK);
    }
}
