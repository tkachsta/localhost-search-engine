package searchengine.services.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.indexer.IndexerMode;
import searchengine.indexer.IndexingUtil;
import searchengine.indexer.IndexerType;
import searchengine.indexer.RunningStatusSync;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.site.IndexingStatus;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.indexer.parser.RecursiveParsingService;
import searchengine.services.IndexingService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);
    private final IndexerType indexerType;
    public final RunningStatusSync runningStatusSync;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;

    @Override
    public boolean startMultipleSitesRecursiveIndexing() {
        if (runningStatusSync.startIsAllowed()) {
            RecursiveParsingService.activateParsing();
            for (Site site : sitesList.getSites()) {
                startSingleSiteRecursiveIndexing(site);
            }
            return true;
        }
        return false;
    }
    public void startSingleSiteRecursiveIndexing(Site site) {
        SiteEntity siteEntity = updateSiteAndPagesOnStart(site);
        indexerType.setIndexerMode(IndexerMode.RECURSIVE);
        indexerType.setPageBatchSize(30);
        IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                indexerType, site, siteEntity, new PageEntity());
        Future<?> f = executorService.submit(indexingUtil);
        runningStatusSync.getFutures().add(f);
    }
    @Override
    public boolean startSinglePageIndexing(String url) {
        String domenRegex = "((?:http|https)://.+(?:ru|com))(/.+)";
        Pattern pattern = Pattern.compile(domenRegex);
        Matcher matcher = pattern.matcher(url);
        matcher.matches();
        String siteUrl = matcher.group(1).trim();
        String pageUrl = matcher.group(2).trim();
        SiteEntity siteEntity = siteRepository.findByUrl(siteUrl);
        PageEntity pageEntity = pageRepository.findByPath(pageUrl);
        if (urlCheckForIndexing(siteUrl)) {
            indexerType.setIndexerMode(IndexerMode.SINGLEPAGE);
            indexerType.setPageBatchSize(1);
            IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                    indexerType, new Site(), siteEntity, pageEntity);
            Thread pageIndexing = new Thread(indexingUtil);
            pageIndexing.start();
            return true;
        }
        return false;
    }
    @Override
    public boolean terminateIndexing() {
        if (!runningStatusSync.startIsAllowed()) {
            RecursiveParsingService.terminateParsing();
            return true;
        }
        return false;
    }
    private SiteEntity updateSiteAndPagesOnStart(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
        List<PageEntity> pageEntityList = pageRepository.findAllBySite(siteEntity);
        indexRepository.removeAllByPages(pageEntityList);
        lemmaRepository.deleteAllBySite(siteEntity);
        pageRepository.deleteAllBySite(siteEntity);
        siteRepository.deleteAllByUrl(site.getUrl());

        siteEntity = new SiteEntity();
        siteEntity.setStatus(IndexingStatus.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteRepository.save(siteEntity);
        return siteEntity;
    }
    private boolean urlCheckForIndexing(String url) {
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }


}


