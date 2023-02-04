package searchengine.controllers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.statistic.StatisticsService;
import searchengine.services.synchronization.StatisticsSynchronization;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StatisticController {
    private final StatisticsService statisticsService;
    private final StatisticsSynchronization synchronization;
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        synchronization.dataBaseSync();
        StatisticsResponse response = statisticsService.getStatistics();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
