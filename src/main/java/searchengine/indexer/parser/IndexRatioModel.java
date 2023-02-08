package searchengine.indexer.parser;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.model.site.IndexingStatus;

@Service
@Setter
@Getter
public class IndexRatioModel {
    private int createdTasks = 0;
    private int completedTasks = 0;
    private IndexingStatus indexingStatus;
    private String message;

    protected void incrementCompletedTask() {
        completedTasks++;
    }
    protected void incrementCreatedTasks() {
        createdTasks++;
    }
    public float getIndexRating() {
        return (float) Math.round(((float) completedTasks / (float) createdTasks) * 100) / 100;
    }
    private void setIndexRatioModelMessage() {
        if (getIndexRating() > 0.7f) {
            indexingStatus = IndexingStatus.INDEXED;
        } else {
            message =  "Проиндексированы не все возможные страницы.";
            indexingStatus = IndexingStatus.FAILED;
        }

    }
    public String getIndexRatioModelMessage() {
        setIndexRatioModelMessage();
        return message;
    }
}
