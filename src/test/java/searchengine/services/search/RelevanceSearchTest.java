package searchengine.services.search;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteRepository;
@SpringBootTest
@DisplayName("Тестирование функции поиска")
class RelevanceSearchTest {
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    SitesList sitesList;

    public void setUp() {

    }


    @Test
    public void getSearchResult() {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery("тональный крем");


        RelevanceSearch relevanceSearch = new RelevanceSearch(pageRepository, lemmaRepository,
                indexRepository,  siteRepository, sitesList);
        SearchResponse searchResponse = relevanceSearch.getSearchResult(searchRequest);
        System.out.println(searchResponse.toString());
    }


}