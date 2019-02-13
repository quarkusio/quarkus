package org.infinispan.protean.hibernate.cache;

public class Time {

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
