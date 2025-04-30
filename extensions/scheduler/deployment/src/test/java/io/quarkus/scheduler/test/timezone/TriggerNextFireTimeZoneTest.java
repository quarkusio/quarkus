package io.quarkus.scheduler.test.timezone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;

public class TriggerNextFireTimeZoneTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Jobs.class);
            });

    @Inject
    Scheduler scheduler;

    @Test
    public void testScheduledJobs() throws InterruptedException {
        Trigger prague = scheduler.getScheduledJob("prague");
        Trigger boston = scheduler.getScheduledJob("boston");
        Trigger ulaanbaatar = scheduler.getScheduledJob("ulaanbaatar");
        assertNotNull(prague);
        assertNotNull(boston);
        assertNotNull(ulaanbaatar);
        Instant pragueNext = prague.getNextFireTime();
        Instant bostonNext = boston.getNextFireTime();
        Instant ulaanbaatarNext = ulaanbaatar.getNextFireTime();
        assertTime(pragueNext.atZone(ZoneId.of("Europe/Prague")));
        assertTime(bostonNext.atZone(ZoneId.of("America/New_York")));
        assertTime(ulaanbaatarNext.atZone(ZoneId.of("Asia/Ulaanbaatar")));
    }

    private static void assertTime(ZonedDateTime time) {
        assertEquals(20, time.getHour());
        assertEquals(30, time.getMinute());
        assertEquals(0, time.getSecond());
    }

    static class Jobs {

        @Scheduled(identity = "prague", cron = "0 30 20 * * ?", timeZone = "Europe/Prague")
        void withPragueTimezone(ScheduledExecution execution) {
            assertNotEquals(execution.getFireTime(), execution.getScheduledFireTime());
            assertTime(execution.getScheduledFireTime().atZone(ZoneId.of("Europe/Prague")));
        }

        @Scheduled(identity = "boston", cron = "0 30 20 * * ?", timeZone = "America/New_York")
        void withBostonTimezone() {
        }

        @Scheduled(identity = "ulaanbaatar", cron = "0 30 20 * * ?", timeZone = "Asia/Ulaanbaatar")
        void withIstanbulTimezone(ScheduledExecution execution) {
            assertTime(execution.getScheduledFireTime().atZone(ZoneId.of("Asia/Ulaanbaatar")));
        }

    }

}
