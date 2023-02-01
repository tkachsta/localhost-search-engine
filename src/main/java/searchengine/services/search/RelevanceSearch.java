package searchengine.services.search;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.DetailedSearchItem;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import searchengine.services.lemmafinder.LemmaFinder;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelevanceSearch implements SearchService {
    private final SearchResponse searchResponse;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaFinder lemmaFinder;
    private final SitesList sitesList;

    public RelevanceSearch(PageRepository pageRepository,
                           LemmaRepository lemmaRepository,
                           IndexRepository indexRepository,
                           SiteRepository siteRepository,
                           SitesList sitesList) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        searchResponse = new SearchResponse();
        lemmaFinder = LemmaFinder.getInstance();
    }
    @Override
    public SearchResponse getSearchResult(SearchRequest searchRequest) {
        Map<String, Integer> queryLemmas = findQueryLemmas(searchRequest.getQuery());
        List<SiteEntity> siteEntityList = getSitesList(searchRequest);
        lemmaTrimming(queryLemmas, 0.50F, siteEntityList);
        Map<String, Integer> sortedLemmaMap = sortedLemmaMap(queryLemmas);
        Map<PageEntity, List<IndexEntity>> indexes = indexesTrimming(sortedLemmaMap, siteEntityList);
        Map<PageEntity, Float> relativeRelevance = getRelativeRelevance(indexes);
        Map<PageEntity, Float> pagesWithHighestRelevance = getPagesWithHighestRelevance(relativeRelevance, searchRequest.getLimit());
        Map<PageEntity, List<IndexEntity>> detailedSearchItems =  processingForDetailedSearchItem(indexes, pagesWithHighestRelevance);
        List<DetailedSearchItem> searchItemList = getDetailedSearchItems(detailedSearchItems, pagesWithHighestRelevance);
        searchResponse.setResult(true);
        searchResponse.setData(searchItemList);
        searchResponse.setCount(detailedSearchItems.size());
        return searchResponse;
    }
    private List<SiteEntity> getSitesList(SearchRequest searchRequest) {
        List<SiteEntity> sites = new ArrayList<>();
        String site = searchRequest.getSite();
        if (site == null) {
            System.out.println("не указан сайт");
            sitesList.getSites().forEach(record -> {
                String siteUrl = record.getUrl();
                SiteEntity siteEntity = siteRepository.findByUrl(siteUrl);
                System.out.println(siteEntity.getUrl());
                sites.add(siteEntity);
            });
            return sites;
        }
        SiteEntity siteEntity = siteRepository.findByUrl(site);
        sites.add(siteEntity);
        sites.add(siteEntity);
        return sites;
    }
    private Map<String, Integer> findQueryLemmas(String query) {
        return lemmaFinder.collectLemmas(query);
    }
    private void lemmaTrimming(Map<String, Integer> queryLemmas, float trimmingCoef, List<SiteEntity> siteEntityList) {
        float totalPages = (float) pageRepository.findCountBySites(siteEntityList);
        Set<String> lemmasToRemove = new HashSet<>();
        queryLemmas.forEach((lemma, rating) -> {
            Optional<Integer> lemmaPresence = lemmaRepository.sumOfLemmaFrequency(lemma);
            if (lemmaPresence.isPresent()) {
                float pagesWithLemma = lemmaPresence.get();
                if ((pagesWithLemma / totalPages) > trimmingCoef) {
                    lemmasToRemove.add(lemma);
                }
            }
        });
        lemmasToRemove.forEach(queryLemmas::remove);
    }
    private Map<String, Integer> sortedLemmaMap(Map<String, Integer> queryLemmas) {
        return queryLemmas.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .limit(20).collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
    private Map<PageEntity, List<IndexEntity>> indexesTrimming(Map<String, Integer> sortedLemma, List<SiteEntity> siteEntityList) {
        List<PageEntity> pageEntityList = new ArrayList<>();
        List<IndexEntity> trimmedIndexes = new ArrayList<>();
        Map<PageEntity, List<IndexEntity>> indexedMap = new HashMap<>();
        Optional<String> firstKey = sortedLemma.keySet().stream().findFirst();
        if (firstKey.isPresent()) {
            String lemma = firstKey.get();
            Collection<IndexEntity> indexEntityList = indexRepository.selectIndexesByKey(lemma, siteEntityList);
            indexEntityList.forEach(index -> pageEntityList.add(index.getPage()));
            sortedLemma.forEach((entry, frequency) -> {
                Collection<IndexEntity> indexes = indexRepository.selectIndexesByLemmaAndPage(entry, pageEntityList, siteEntityList);
                pageEntityList.clear();
                indexes.forEach(index -> pageEntityList.add(index.getPage()));
                trimmedIndexes.addAll(indexes);

            });
            pageEntityList.forEach(page -> {
                List<IndexEntity> list = new ArrayList<>();
                trimmedIndexes.forEach(index -> {
                    if (page.equals(index.getPage())) {
                        list.add(index);
                    }
                });
                indexedMap.put(page, list);
            });
        }
        return indexedMap;
    }
    private Map<PageEntity, List<IndexEntity>> processingForDetailedSearchItem(Map<PageEntity, List<IndexEntity>> trimmedIndexes,
                                                                               Map<PageEntity, Float> pagesWithHighestRelevance) {
        Map<PageEntity, List<IndexEntity>> forDetailedSearchItem = new HashMap<>();
        pagesWithHighestRelevance.forEach((page, freq) -> forDetailedSearchItem.put(page, trimmedIndexes.get(page)));
        return forDetailedSearchItem;
    }
    private List<DetailedSearchItem> getDetailedSearchItems(Map<PageEntity, List<IndexEntity>> indexedMap,
                                                            Map<PageEntity, Float> pagesWithHighestRelevance) {
        List<DetailedSearchItem> searchPages = new ArrayList<>();
        indexedMap.forEach((page,v) -> {
            DetailedSearchItem searchItem = new DetailedSearchItem();
            searchItem.setUri(page.getPath());
            searchItem.setSite(page.getSite().getUrl());
            searchItem.setSiteName(page.getSite().getName());
            searchItem.setTitle(getPageTitle(page));
            searchItem.setSnippet(getPageSnippet(page, v));
            searchItem.setRelevance(getPageRelevance(pagesWithHighestRelevance, page));
            searchPages.add(searchItem);
        });
        return searchPages;
    }
    private Map<PageEntity, Float> getRelativeRelevance(Map<PageEntity, List<IndexEntity>> indexedMap) {
        Map<PageEntity, Float> absPageRelevance = new HashMap<>();
        Map<PageEntity, Float> relativePageRelevance = new HashMap<>();
        float maxRelevance = 0;
        indexedMap.forEach((page, indexList) -> {
            float absRelevance = (float) 0;
            for (IndexEntity index : indexList) {
                absRelevance += index.getRank();
            }
            absPageRelevance.put(page, absRelevance);
        });
        for (Map.Entry<PageEntity, Float> map : absPageRelevance.entrySet()) {
            if (map.getValue() > maxRelevance) {
                maxRelevance = map.getValue();
            }
        }
        for (Map.Entry<PageEntity, Float> map : absPageRelevance.entrySet()) {
            relativePageRelevance.put(map.getKey(), map.getValue() / maxRelevance);
        }
        return relativePageRelevance;
    }
    private Map<PageEntity, Float> getPagesWithHighestRelevance(Map<PageEntity, Float> relativeRelevance, int limit) {
        return relativeRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit).collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
    private String getPageTitle(PageEntity page) {
        Document doc = Jsoup.parse(page.getContent());
        return doc.title();
    }
    private String getPageSnippet(PageEntity page, List<IndexEntity> indexes) {
        Document doc = Jsoup.parse(page.getContent());
        String text = doc.text();
        List<String> splitText = new ArrayList<>(Arrays.asList(text.split(" ")));
        int position = 0;
        for (int i = 0; i < splitText.size(); i++) {
            String word = splitText.get(i);
            Set<String> lemmaSet = lemmaFinder.getLemmaSet(word);
            Optional<String> lemmaWord = lemmaSet.stream().findFirst();
            for (IndexEntity index : indexes) {
                if (lemmaWord.isPresent()) {
                    if (lemmaWord.get().equals(index.getLemma().getLemma())) {
                        splitText.set(i, "<b>" + word + "</b>");
                        position = i;
                    }
                }
            }
        }
        List<String> snippetList;
        if (position < 15) {
            snippetList = splitText.subList(0, 30);
        } else if (position > splitText.size() - 15) {
            snippetList = splitText.subList(splitText.size() - 30, splitText.size());
        } else {
            snippetList = splitText.subList(position - 15, position + 15);
        }
        return String.join(" ", snippetList);
    }
    private float getPageRelevance(Map<PageEntity, Float> relativePageRelevance, PageEntity page) {
        return relativePageRelevance.get(page);
    }

}
