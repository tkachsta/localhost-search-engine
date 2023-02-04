package searchengine.controllers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.searchrequest.SearchRequest;
import searchengine.dto.searchresponse.FalseResponse;
import searchengine.services.search.SearchService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {
    private final SearchService searchService;
    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "offset", required = false, defaultValue = "") int offset,
            @RequestParam(name = "limit", required = false, defaultValue = "") int limit,
            @RequestParam(name = "site", required = false, defaultValue = "") String site) {
        if (query.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Задан пустой поисковой запрос"),
                    HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(searchService.getSearchResult(new SearchRequest(query, site, offset, limit)));
    }
}
