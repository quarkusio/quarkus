package org.infinispan.quarkus.hibernate.cache;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

final class Time {

    private static final Duration FOREVER = ChronoUnit.FOREVER.getDuration();

    private Time() {
    }

    static Duration forever() {
        return FOREVER;
    }

    static boolean isForever(Duration duration) {
        return FOREVER.equals(duration);
    }

    @FunctionalInterface
    public interface NanosService {
        long nanoTime();
    }

    @FunctionalInterface
    public interface MillisService {
        MillisService SYSTEM = System::currentTimeMillis;

        long milliTime();
    }

}
