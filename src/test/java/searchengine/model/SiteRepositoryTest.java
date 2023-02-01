package searchengine.model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.site.IndexingStatus;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@SpringBootTest
@DisplayName("Интеграция SiteRepository с БД")
@TestClassOrder(ClassOrderer.class)
@Order(1)
class SiteRepositoryTest {
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    int numberOfEntries = 0;
    int numberOfIndexedEntries = 0;
    @BeforeEach
    public void setUp() {
        String path = "src/test/java/searchengine/model/sites.json";
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonFile = Files.readString(Paths.get(path));
            JsonNode nodes = objectMapper.readTree(jsonFile);
            for (JsonNode entry : nodes) {
                SiteEntity site = new SiteEntity();
                site.setStatus(IndexingStatus.valueOf(entry.get("status").asText()));
                site.setStatusTime(LocalDateTime.parse(entry.get("statusTime").asText()));
                site.setLastError(entry.get("lastError").asText());
                site.setUrl(entry.get("url").asText());
                site.setName(entry.get("name").asText());

                if (entry.get("status").asText().equals("INDEXED")) {
                    numberOfIndexedEntries++;
                }

                numberOfEntries++;
                siteRepository.save(site);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Test
    @DisplayName("INSERT в таблицу SiteRepository")
    public void writeToSiteTable() {
        int numberOfEntriesFromJSON = numberOfEntries;
        int numberOfEntriesFromDB = siteRepository.findCount();
        Assertions.assertEquals(numberOfEntriesFromJSON, numberOfEntriesFromDB);
    }
    @Test
    @DisplayName("SELECT по полю Статус")
    public void getByStatus() {
        Iterable<SiteEntity> sites = siteRepository.findByStatus(IndexingStatus.INDEXED);
        int numberOfIndexedEntriesFromJSON = numberOfIndexedEntries;
        int numberOfIndexedEntriesFromDB = 0;
        for (SiteEntity site : sites) {
            numberOfIndexedEntriesFromDB++;
        }
        Assertions.assertEquals(numberOfIndexedEntriesFromJSON,
                numberOfIndexedEntriesFromDB);
    }
    @Test
    @DisplayName("SELECT по полю URL сайта")
    public void getByUrl() {

        Assertions.assertEquals("test_error_26",
                siteRepository.findByUrl("test_url_6").getLastError());
    }
    @Test
    @DisplayName("Удаление из БД по URL сайта")
    public void removeAllByUrl() {
        siteRepository.removeAllByUrl("test_url_9");
        Assertions.assertEquals(9, siteRepository.findCount());
    }

}