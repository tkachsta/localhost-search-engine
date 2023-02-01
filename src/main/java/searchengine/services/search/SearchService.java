package searchengine.services.search;

import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse getSearchResult(SearchRequest searchRequest);
}
