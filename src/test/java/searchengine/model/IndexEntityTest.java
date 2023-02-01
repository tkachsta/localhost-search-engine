package searchengine.model;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.model.index.IndexEntity;
import searchengine.model.index.IndexKey;
import searchengine.model.index.IndexRepository;
import searchengine.model.lemma.LemmaEntity;
import searchengine.model.lemma.LemmaRepository;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import searchengine.model.site.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@DisplayName("Интеграция IndexRepository с БД")
@TestClassOrder(ClassOrderer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Order(4)
class IndexEntityTest {

    @Autowired
    PageRepository pageRepository;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;

    @Test
    @Order(1)
    @DisplayName("Создание индексов в таблице Index")
    public void writeToPageTable() {
        int maxId = pageRepository.findMaxId();
        for (int i = 0; i < 300; i++) {
            int randomPage = maxId - (int) (Math.random() * 10);
            int randomLemma = (int) (Math.random() * 9) + 1;

            Optional<PageEntity> page = pageRepository.findById(randomPage);
            Optional<LemmaEntity> lemma = lemmaRepository.findById(randomLemma);
            float frequency = (int) (Math.random() * 10) + 1;
            IndexEntity index = new IndexEntity();
            IndexKey indexKey = new IndexKey(page.get(), lemma.get());
            index.setIndex_id(indexKey);
            index.setRank(frequency);
            indexRepository.save(index);
        }

    }
    @Test
    @Order(2)
    @DisplayName("SELECT всех IndexEntity по PageEntity")
    public void findLemmasByPage() {
        int maxId = pageRepository.findMaxId();
        PageEntity pageEntity = pageRepository.findById(maxId).get();
        List<IndexEntity> indexEntityList = indexRepository.selectByPageId(pageEntity);
        Assertions.assertFalse(indexEntityList.isEmpty());
    }
    @Test
    @Order(3)
    @DisplayName("DELETE LemmeEntity по PageEntity")
    public void removeLemmasForPage() {
        int maxId = pageRepository.findMaxId();
        int iniFreq = lemmaRepository.totalSumOfFrequency();
        PageEntity pageEntity = pageRepository.findById(maxId).get();
        List<IndexEntity> indexEntityList = indexRepository.selectByPageId(pageEntity);
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        int sumRank = 0;
        for (IndexEntity index : indexEntityList) {
            sumRank += index.getRank();
            LemmaEntity lemmaEntity = index.getLemma();
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() - (int) index.getRank());
            lemmaEntityList.add(lemmaEntity);
        }
        lemmaRepository.saveAll(lemmaEntityList);
        int endFreq = lemmaRepository.totalSumOfFrequency();
        Assertions.assertEquals(iniFreq, endFreq + sumRank);
    }
    @Test
    @Order(4)
    @DisplayName("DELETE всех IndexEntity по PageEntity")
    public void removeLemmasByPage() {
        int maxId = pageRepository.findMaxId();
        PageEntity pageEntity = pageRepository.findById(maxId).get();
        indexRepository.removeAllByPage(pageEntity);
        List<IndexEntity> indexEntityList = indexRepository.selectByPageId(pageEntity);
        Assertions.assertTrue(indexEntityList.isEmpty());
    }
}