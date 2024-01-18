package io.quarkus.scheduler.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.common.runtime.SyntheticScheduled;

public class SyntheticScheduledTest {

    @Test
    public void testJson() {
        SyntheticScheduled s1 = new SyntheticScheduled("foo", "", "2s", 0, TimeUnit.SECONDS, "1s", "15m",
                ConcurrentExecution.PROCEED, null, Scheduled.DEFAULT_TIMEZONE);
        SyntheticScheduled s2 = SyntheticScheduled.fromJson(s1.toJson());
        assertEquals(s1.identity(), s2.identity());
        assertEquals(s1.concurrentExecution(), s2.concurrentExecution());
        assertEquals(s1.cron(), s2.cron());
        assertEquals(s1.every(), s2.every());
        assertEquals(s1.delay(), s2.delay());
        assertEquals(s1.delayUnit(), s2.delayUnit());
        assertEquals(s1.delayed(), s2.delayed());
        assertEquals(s1.overdueGracePeriod(), s2.overdueGracePeriod());
        assertEquals(s1.timeZone(), s2.timeZone());
    }

}
