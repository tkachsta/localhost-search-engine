package searchengine.services.schedulers;
import searchengine.model.site.SiteEntity;
import searchengine.model.site.SiteRepository;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class SiteEntityScheduler {
    SiteEntity siteEntity;
    private final SiteRepository siteRepository;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(5);
    public SiteEntityScheduler(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }
    public void setSiteEntity(SiteEntity siteEntity) {
        this.siteEntity = siteEntity;
    }
    public ScheduledFuture<?> updateTimeOnSchedule() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);
            }
        };
        return scheduler.scheduleAtFixedRate(runnable, 15, 15, TimeUnit.SECONDS);
    }
    public ScheduledFuture<?> updateSiteEntity(SiteEntity siteEntity) {
        Runnable runnable = () -> siteEntity.setStatusTime(LocalDateTime.now());
        return scheduler.scheduleAtFixedRate(runnable, 15, 15, TimeUnit.SECONDS);
    }
}
