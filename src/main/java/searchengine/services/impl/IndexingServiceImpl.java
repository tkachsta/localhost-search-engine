package searchengine.services.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.indexer.IndexingRunningStatus;
import searchengine.indexer.IndexingUtil;
import searchengine.indexer.ParserType;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.site.IndexingStatus;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.parser.RecursiveParsingService;
import searchengine.services.IndexingService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static IndexingRunningStatus runningStatus = IndexingRunningStatus.IDLE;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;

    @Override
    public boolean startMultipleSitesRecursiveIndexing() {
        if (runningStatus == IndexingRunningStatus.RUNNING) {
            return false;
        } else {
            runningStatus = IndexingRunningStatus.RUNNING;
            RecursiveParsingService.activateParsing();
            for (Site site : sitesList.getSites()) {
                Thread thread = new Thread(() -> startSingleSiteRecursiveIndexing(site));
                thread.start();
            }
        }
        return true;
    }
    public void startSingleSiteRecursiveIndexing(Site site) {
        SiteEntity siteEntity = updateSiteAndPagesOnStart(site);
        IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                ParserType.SINGLESITE, site, siteEntity, new PageEntity());
        Thread siteIndexing = new Thread(indexingUtil);
        siteIndexing.start();
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
        PageEntity pageEntity = pageRepository.findPageByPath(pageUrl);
        if (urlCheckForIndexing(siteUrl)) {
            IndexingUtil indexingUtil = new IndexingUtil(siteRepository, pageRepository, lemmaRepository, indexRepository,
                    ParserType.SINGLEPAGE, new Site(), siteEntity, pageEntity);
            Thread pageIndexing = new Thread(indexingUtil);
            pageIndexing.start();
            return true;
        }
        return false;
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
    private boolean urlCheckForIndexing(String url) {
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }
    public static IndexingRunningStatus getRunningStatus() {
        return runningStatus;
    }
}


