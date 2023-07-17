package io.quarkus.spring.scheduled.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import org.springframework.scheduling.annotation.Scheduled;

@ApplicationScoped
public class SpringScheduledMethodsBean {

    static final Map<String, CountDownLatch> LATCHES;

    static {
        LATCHES = new ConcurrentHashMap<>();
        LATCHES.put("fixedRate", new CountDownLatch(2));
        LATCHES.put("fixedRateConfig", new CountDownLatch(2));
        LATCHES.put("fixedRateString", new CountDownLatch(2));
        LATCHES.put("cron", new CountDownLatch(2));
        LATCHES.put("cronConfig", new CountDownLatch(2));
    }

    @Scheduled(cron = "0/1 * * * * ?")
    void checkEverySecondCron() {
        LATCHES.get("cron").countDown();
    }

    @Scheduled(fixedRate = 1000)
    void checkEverySecond() {
        LATCHES.get("fixedRate").countDown();
    }

    @Scheduled(cron = "${springScheduledSimpleJobs.cron}")
    void checkEverySecondCronConfig() {
        LATCHES.get("cronConfig").countDown();
    }

    @Scheduled(fixedRateString = "${springScheduledSimpleJobs.fixedRate}")
    void checkEverySecondConfig() {
        LATCHES.get("fixedRateConfig").countDown();
    }

    @Scheduled(fixedRateString = "1000")
    void checkEverySecondAsString() {
        LATCHES.get("fixedRateString").countDown();
    }

}
