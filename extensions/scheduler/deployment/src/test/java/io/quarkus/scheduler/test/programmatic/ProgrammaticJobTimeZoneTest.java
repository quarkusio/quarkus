package io.quarkus.scheduler.test.programmatic;

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

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class ProgrammaticJobTimeZoneTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(new StringAsset("quarkus.scheduler.start-mode=forced"), "application.properties"));

    @Inject
    Scheduler scheduler;

    static final CountDownLatch LATCH = new CountDownLatch(2);
    static final CountDownLatch TIME_ZONE_1_LATCH = new CountDownLatch(1);
    static final CountDownLatch TIME_ZONE_2_LATCH = new CountDownLatch(1);

    @Test
    public void testJobs() throws InterruptedException {
        ZonedDateTime now = ZonedDateTime.now();
        String timeZone = findTimeZoneWithOffset(now);
        int job2Hour = now.withZoneSameInstant(ZoneId.of(timeZone)).getHour();

        scheduler.newJob("simpleJobs0").setInterval("1s").setTask(ec -> {
            LATCH.countDown();
        }).schedule();

        scheduler.newJob("simpleJobs1").setCron(String.format("0/1 * %s * * ?", now.getHour())).setTimeZone(timeZone)
                .setTask(ec -> {
                    // this method should not be executed in the test
                    TIME_ZONE_1_LATCH.countDown();
                }).schedule();

        scheduler.newJob("simpleJobs2").setCron(String.format("0/1 * %s * * ?", job2Hour)).setTimeZone(timeZone)
                .setTask(ec -> {
                    TIME_ZONE_2_LATCH.countDown();
                }).schedule();

        assertTrue(LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(TIME_ZONE_2_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals(1l, TIME_ZONE_1_LATCH.getCount());
    }

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

}
