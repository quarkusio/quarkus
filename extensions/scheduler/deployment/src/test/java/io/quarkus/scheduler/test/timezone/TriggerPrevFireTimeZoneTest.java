package io.quarkus.scheduler.test.timezone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;

public class TriggerPrevFireTimeZoneTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime prague = now.withZoneSameInstant(ZoneId.of("Europe/Prague"));
                ZonedDateTime istanbul = now.withZoneSameInstant(ZoneId.of("Europe/Istanbul"));
                // For example, the current date-time is 2024-07-09 10:08:00;
                // the default time zone is Europe/London
                // then the config should look like:
                // simpleJobs1.cron=0/1 * 11 * * ?
                // simpleJobs2.cron=0/1 * 12 * * ?
                String properties = String.format(
                        "simpleJobs1.cron=0/1 * %s * * ?\n"
                                + "simpleJobs1.hour=%s\n"
                                + "simpleJobs2.cron=0/1 * %s * * ?\n"
                                + "simpleJobs2.hour=%s",
                        prague.getHour(), prague.getHour(), istanbul.getHour(), istanbul.getHour());
                root.addClasses(Jobs.class)
                        .addAsResource(
                                new StringAsset(properties),
                                "application.properties");
            });

    @ConfigProperty(name = "simpleJobs1.hour")
    int pragueHour;

    @ConfigProperty(name = "simpleJobs2.hour")
    int istanbulHour;

    @Inject
    Scheduler scheduler;

    @Test
    public void testScheduledJobs() throws InterruptedException {
        assertTrue(Jobs.PRAGUE_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.ISTANBUL_LATCH.await(5, TimeUnit.SECONDS));
        Trigger prague = scheduler.getScheduledJob("prague");
        Trigger istanbul = scheduler.getScheduledJob("istanbul");
        assertNotNull(prague);
        assertNotNull(istanbul);
        Instant praguePrev = prague.getPreviousFireTime();
        Instant istanbulPrev = istanbul.getPreviousFireTime();
        assertNotNull(praguePrev);
        assertNotNull(istanbulPrev);
        assertEquals(praguePrev, istanbulPrev);
        assertEquals(pragueHour, praguePrev.atZone(ZoneId.of("Europe/Prague")).getHour());
        assertEquals(istanbulHour, istanbulPrev.atZone(ZoneId.of("Europe/Istanbul")).getHour());
    }

    static class Jobs {

        static final CountDownLatch PRAGUE_LATCH = new CountDownLatch(1);
        static final CountDownLatch ISTANBUL_LATCH = new CountDownLatch(1);

        @Scheduled(identity = "prague", cron = "{simpleJobs1.cron}", timeZone = "Europe/Prague")
        void withPragueTimezone() {
            PRAGUE_LATCH.countDown();
        }

        @Scheduled(identity = "istanbul", cron = "{simpleJobs2.cron}", timeZone = "Europe/Istanbul")
        void withIstanbulTimezone() {
            ISTANBUL_LATCH.countDown();
        }

    }

}
