package io.quarkus.proxy.test.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class XMessageUtil {
    static final Instant T0 = Instant.parse("1980-01-06T00:00:00Z");

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    public static Instant decodeData(byte[] data) {
        int sec = ((data[0] & 0xFF) << 24) |
            ((data[1] & 0xFF) << 16) |
            ((data[2] & 0xFF) << 8) |
            (data[3] & 0xFF);
        return T0.plusSeconds((long) sec & 0xFFFFFFFFL);
    }

    public static byte[] encodeData(Instant instant) {
        int sec = (int) T0.until(instant, ChronoUnit.SECONDS);
        return new byte[] {
            (byte) (sec >>> 24),
            (byte) (sec >>> 16),
            (byte) (sec >>> 8),
            (byte) sec
        };
    }
}
