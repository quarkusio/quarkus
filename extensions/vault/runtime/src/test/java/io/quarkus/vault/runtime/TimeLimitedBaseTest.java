package io.quarkus.vault.runtime;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class TimeLimitedBaseTest {

    @Test
    public void isExpired() {
        assertFalse(newTimeLimitedBase(true, 1, EPOCH, EPOCH.plusMillis(1000)).isExpired());
        assertTrue(newTimeLimitedBase(true, 1, EPOCH, EPOCH.plusMillis(2001)).isExpired());
    }

    @Test
    public void shouldExtend() {
        assertFalse(newTimeLimitedBase(true, 80, EPOCH, EPOCH.plusSeconds(60)).shouldExtend(ofSeconds(10)),
                "before grace period");
        assertTrue(newTimeLimitedBase(true, 80, EPOCH, EPOCH.plusSeconds(71)).shouldExtend(ofSeconds(10)),
                "within grace period");
        assertFalse(newTimeLimitedBase(true, 80, EPOCH, EPOCH.plusSeconds(82)).shouldExtend(ofSeconds(10)), "expired");
        assertFalse(newTimeLimitedBase(false, 80, EPOCH, EPOCH.plusSeconds(71)).shouldExtend(ofSeconds(10)),
                "within grace period, but not renewable");
    }

    @Test
    public void expiresSoon() {
        assertFalse(newTimeLimitedBase(true, 80, EPOCH, EPOCH.plusSeconds(60)).expiresSoon(ofSeconds(10)),
                "lease duration > grace period");
        assertTrue(newTimeLimitedBase(true, 9, EPOCH, EPOCH.plusSeconds(60)).expiresSoon(ofSeconds(10)),
                "lease duration < grace period");
    }

    @Test
    public void getExpiredDate() {
        assertEquals("1970-01-01T00:01:20Z",
                newTimeLimitedBase(true, 80, EPOCH, EPOCH.plusSeconds(60)).getExpireInstant().toString());
    }

    private TimeLimitedBase newTimeLimitedBase(boolean renewable, long leaseDurationSecs, Instant created, Instant now) {
        TimeLimitedBase timeLimitedBase = new TimeLimitedBase(renewable, leaseDurationSecs) {
        };
        timeLimitedBase.created = created;
        timeLimitedBase.nowSupplier = () -> now;
        return timeLimitedBase;
    }

}
