package searchengine.services.schedulers;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class DataProcessorScheduler {

    public boolean batchTimer;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    public ScheduledFuture<?> scheduleBatches() {
        final Runnable runnable = () -> {
            System.out.println(batchTimer);
            batchTimer = true;
        };
        return scheduler.scheduleAtFixedRate(runnable, 30, 20, TimeUnit.SECONDS);
    }
}
