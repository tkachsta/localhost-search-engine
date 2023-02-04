package searchengine.services.lemmafinder;
import org.apache.lucene.morphology.LuceneMorphology;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.lemmafinder.LemmaFinder;
import searchengine.model.page.PageEntity;
import searchengine.model.page.PageRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
@DisplayName("Лемматизация слов")
class LemmaFinderTest {

    @Autowired
    PageRepository pageRepository;
    @Autowired
    LuceneMorphology luceneMorphology;

    @Test
    public void testLemma() {
        Optional<PageEntity> pageEntity = pageRepository.findById(71);
        LemmaFinder lf = LemmaFinder.getInstance();
        String text = pageEntity.get().getContent();
        HashMap<String, Integer> lemmas = new HashMap<>(lf.collectLemmas(text));
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

}