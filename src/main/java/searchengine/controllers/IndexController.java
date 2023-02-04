package searchengine.controllers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.searchresponse.FalseResponse;
import searchengine.dto.searchresponse.TrueResponse;
import searchengine.services.IndexingService;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IndexController {
    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.startMultipleSitesRecursiveIndexing()) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new FalseResponse(false, "Индексация уже запущена."),
                HttpStatus.OK);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (IndexingService.terminateIndexing()) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        } else return new ResponseEntity<>(new FalseResponse(false, "индексация не запущена"), HttpStatus.OK);
    }
    @PostMapping(value = "/indexPage")
    public ResponseEntity<Object> startPageIndexing(@RequestParam String url) {
        if (indexingService.startSinglePageIndexing(url)) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        } else return new ResponseEntity<>(new FalseResponse(false,
                "Данная страница находится за пределами сайтов указанных в конфигурации."), HttpStatus.OK);
    }
}
