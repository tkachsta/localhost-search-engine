package searchengine.indexer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@Getter
@Setter
@Component
public class RunningStatusSync {
    private final List<Future<?>> futures = new ArrayList<>();
    public boolean startIsAllowed() {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }
}
