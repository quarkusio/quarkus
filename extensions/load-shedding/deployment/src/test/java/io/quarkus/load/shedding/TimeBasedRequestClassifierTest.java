package io.quarkus.load.shedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class TimeBasedRequestClassifierTest {
    @Test
    public void fixedTime() {
        fixedTime(0);
        fixedTime(1_000);
        fixedTime(500_000);
    }

    private void fixedTime(long now) {
        int hour = (int) (now >> 22);

        int nextHour = (int) ((now + 1_000) >> 22);
        assertEquals(hour, nextHour);

        nextHour = (int) ((now + 1_000_000) >> 22);
        assertEquals(hour, nextHour);

        nextHour = (int) ((now + 3_600_000) >> 22);
        assertEquals(hour, nextHour);

        // 4_200_000 because 2^22 = 4_194_304
        nextHour = (int) ((now + 4_200_000) >> 22);
        assertNotEquals(hour, nextHour);
    }

    @Test
    public void currentTime() {
        long now = System.currentTimeMillis();
        int hour = (int) (now >> 22);

        int nextHour = (int) ((now + 4_200_000) >> 22);
        assertNotEquals(hour, nextHour);
    }
}
