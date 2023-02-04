package searchengine.services;

import searchengine.dto.searchrequest.SearchRequest;
import searchengine.dto.searchresponse.SearchResponse;

public interface SearchService {
    SearchResponse getSearchResult(SearchRequest searchRequest);
}
