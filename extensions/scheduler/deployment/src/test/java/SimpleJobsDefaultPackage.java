import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.quarkus.scheduler.Scheduled;

public class SimpleJobsDefaultPackage {

    static final Map<String, CountDownLatch> LATCHES;

    static {
        LATCHES = new ConcurrentHashMap<>();
        LATCHES.put("every", new CountDownLatch(2));
        LATCHES.put("everyConfig", new CountDownLatch(2));
        LATCHES.put("cron", new CountDownLatch(2));
        LATCHES.put("cronConfig", new CountDownLatch(2));
    }

    // the cron expression takes precedence
    @Scheduled(cron = "0/1 * * * * ?", every = "2m")
    void checkEverySecondCron() {
        LATCHES.get("cron").countDown();
    }

    @Scheduled(every = "1s")
    void checkEverySecond() {
        LATCHES.get("every").countDown();
    }

    @Scheduled(cron = "{simpleJobs.cron}")
    void checkEverySecondCronConfig() {
        LATCHES.get("cronConfig").countDown();
    }

    @Scheduled(every = "{simpleJobs.every}")
    void checkEverySecondConfig() {
        LATCHES.get("everyConfig").countDown();
    }

}
