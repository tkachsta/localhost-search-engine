package searchengine.model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageRepository;
import searchengine.model.site.IndexingStatus;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
@DisplayName("Интеграция LemmaRepository с БД")
@TestClassOrder(ClassOrderer.class)
@Order(3)
class LemmaEntityTest {
    @Autowired
    PageRepository pageRepository;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    int numberOfEntries = 0;
    int numberOfSites = 0;
    @BeforeEach
    public void setUp() {

    }
    @Test
    @DisplayName("Запись данных в таблицу Lemma")
    public void writeToPageTable() {
        String path = "src/test/java/searchengine/model/lemmas.json";
        ObjectMapper objectMapper = new ObjectMapper();
        List<SiteEntity> sites = siteRepository.findAllByStatus(IndexingStatus.INDEXED);
        numberOfSites = sites.size();

        try {
            int iterationCount = 0;
            String jsonFile = Files.readString(Paths.get(path));
            JsonNode nodes = objectMapper.readTree(jsonFile);
            System.out.println(nodes.size());
            for (JsonNode entry : nodes) {
                System.out.println(entry.get("lemma").asText());
                LemmaEntity lemma = new LemmaEntity();
                lemma.setLemma(entry.get("lemma").asText());
                lemma.setFrequency(entry.get("frequency").asInt());
                if (iterationCount < nodes.size() / 2) {
                    lemma.setSite(sites.get(0));
                } else {
                    lemma.setSite(sites.get(1));
                }

                iterationCount++;
                numberOfEntries++;
                lemmaRepository.save(lemma);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int numberOfEntriesFromJSON = numberOfEntries;
        int numberOfEntriesFromDB = lemmaRepository.countAllBy();
        Assertions.assertEquals(numberOfEntriesFromJSON, numberOfEntriesFromDB);
    }
    @Test
    @DisplayName("Мэппинг таблицы Lemma на таблицу Site")
    public void pageMappingToSite() {
        Assertions.assertEquals(numberOfSites,
                lemmaRepository.findUniqNumberOfSites());
    }
}