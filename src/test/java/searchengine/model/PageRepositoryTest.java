package searchengine.model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.model.site.IndexingStatus;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SpringBootTest
@DisplayName("Интеграция PageRepository с БД")
@TestClassOrder(ClassOrderer.class)
@Order(2)
class PageRepositoryTest {
    @Autowired
    PageRepository pageRepository;
    @Autowired
    SiteRepository siteRepository;
    int numberOfEntries = 0;
    int numberOfSites = 0;

    @BeforeEach
    public void setUp() {
        String path = "src/test/java/searchengine/model/pages.json";
        pageRepository.deleteAll();
        ObjectMapper objectMapper = new ObjectMapper();

        Iterable<SiteEntity> sites = siteRepository.findByStatus(IndexingStatus.INDEXED);
        List<SiteEntity> sitesList = new ArrayList<>((Collection) sites);
        numberOfSites = sitesList.size();

        try {
            int iterationCount = 0;
            String jsonFile = Files.readString(Paths.get(path));
            JsonNode nodes = objectMapper.readTree(jsonFile);
            for (JsonNode entry : nodes) {
                PageEntity page = new PageEntity();

                page.setCode(entry.get("code").asInt());
                page.setPath(entry.get("path").asText());
                page.setContent(entry.get("content").asText());

                if (iterationCount < nodes.size() / 2) {
                    page.setSite(sitesList.get(0));
                } else {
                    page.setSite(sitesList.get(1));
                }

                iterationCount++;
                numberOfEntries++;
                pageRepository.save(page);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Test
    @DisplayName("Запись данных в таблицу Page")
    public void writeToPageTable() {
        int numberOfEntriesFromJSON = numberOfEntries;
        int numberOfEntriesFromDB = pageRepository.findCount();
        Assertions.assertEquals(numberOfEntriesFromJSON, numberOfEntriesFromDB);
    }
    @Test
    @DisplayName("Мэппинг таблицы Page на таблицу Site")
    public void pageMappingToSite() {
        Assertions.assertEquals(numberOfSites,
                pageRepository.findUniqNumberOfSites());
    }
    @Test
    @DisplayName("Проверка наличия Page в БД")
    public void pageExistsInPageEntity() {
        Assertions.assertTrue(
                pageRepository.findPressenceOfPage("path1") > 0);
    }
    @Test
    @DisplayName("Проверка отсутствия Page в БД")
    public void pageNotExistsInPageEntity() {
        Assertions.assertEquals(0, pageRepository.findPressenceOfPage("path"));
    }
    @Test
    @DisplayName("Проверка запроса нескольких записей по Path")
    public void getMultipleInsertByPath() {
        List<String> pathes = new ArrayList<>();
        pathes.add("path1");
        pathes.add("path2");
        pathes.add("path3");
        pathes.add("path12");
        pathes.add("path13");
        pathes.add("path14");

        List<PageEntity> queries = pageRepository.multipleSelectByPath(pathes);
        List<String> responseList = new ArrayList<>();
        queries.forEach(x -> responseList.add(x.getPath()));
        pathes.removeAll(responseList);

        Assertions.assertEquals(3, pathes.size());
    }
    @Test
    @DisplayName("Удаление записей по сущности SiteEntity")
    public void removeAllBySite() {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setStatus(IndexingStatus.FAILED);
        siteEntity.setStatusTime(LocalDateTime.parse("2020-09-12T22:15:15"));
        siteEntity.setLastError("test_error_1");
        siteEntity.setUrl("test_url_15");
        siteEntity.setName("test_name_1");
        siteRepository.save(siteEntity);
        for (int i = 11; i < 16; i++) {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setSite(siteEntity);
            pageEntity.setContent("test_content_25");
            pageEntity.setCode(505);
            pageEntity.setPath("test_path_25");
        }
        pageRepository.removeAllBySite(siteEntity);
        Assertions.assertEquals(10, pageRepository.findCount());
    }


}