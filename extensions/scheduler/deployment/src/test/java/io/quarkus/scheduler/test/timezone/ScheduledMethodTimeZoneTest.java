package io.quarkus.scheduler.test.timezone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class ScheduledMethodTimeZoneTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                ZonedDateTime now = ZonedDateTime.now();
                String timeZone = findTimeZoneWithOffset(now);
                int job2Hour = now.withZoneSameInstant(ZoneId.of(timeZone)).getHour();
                // For example, the current date-time is 2023-02-22 12:05:00;
                // the default time zone is Europe/Prague and the offset time zone is Australia/Sydney
                // then the config should look like:
                // simpleJobs1.cron=0/1 * 12 * * ?
                // simpleJobs1.timeZone=Australia/Sydney
                // simpleJobs2.cron=0/1 * 22 * * ?
                // simpleJobs2.timeZone=Australia/Sydney
                String properties = String.format(
                        "simpleJobs1.cron=0/1 * %s * * ?\n"
                                + "simpleJobs1.timeZone=%s\n"
                                + "simpleJobs2.cron=0/1 * %s * * ?\n"
                                + "simpleJobs2.timeZone=%s",
                        now.getHour(), timeZone, job2Hour, timeZone);
                jar.addClasses(Jobs.class)
                        .addAsResource(
                                new StringAsset(properties),
                                "application.properties");
            });

    @Inject
    Scheduler scheduler;

    static String findTimeZoneWithOffset(ZonedDateTime now) {
        // Try to find a time zone with offset = defaultOffset + 2 hours (at least)
        int defaultOffset = now.getOffset().getTotalSeconds();
        Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();
        int limit = 60 * 60 * 2;
        for (String id : availableZoneIds) {
            int offset = now.withZoneSameLocal(ZoneId.of(id)).getOffset().getTotalSeconds();
            if ((offset - defaultOffset) > limit) {
                return id;
            }
        }
        throw new IllegalStateException("No suitable time zone found");
    }

    @Test
    public void testScheduledJobs() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.TIME_ZONE_2_LATCH.await(5, TimeUnit.SECONDS));
        // checkEverySecondCronTimeZone1() should not be executed because its cron is associated with a different time zone
        assertEquals(1l, Jobs.TIME_ZONE_1_LATCH.getCount());
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(2);
        static final CountDownLatch TIME_ZONE_1_LATCH = new CountDownLatch(1);
        static final CountDownLatch TIME_ZONE_2_LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s")
        void checkEverySecond() {
            LATCH.countDown();
        }

        @Scheduled(identity = "simpleJobs1", cron = "{simpleJobs1.cron}", timeZone = "{simpleJobs1.timeZone}")
        void checkEverySecondCronTimeZone1(ScheduledExecution execution) {
            // this method should not be executed in the test
            TIME_ZONE_1_LATCH.countDown();
        }

        @Scheduled(identity = "simpleJobs2", cron = "{simpleJobs2.cron}", timeZone = "{simpleJobs2.timeZone}")
        void checkEverySecondCronTimeZone2() {
            TIME_ZONE_2_LATCH.countDown();
        }

    }

}
