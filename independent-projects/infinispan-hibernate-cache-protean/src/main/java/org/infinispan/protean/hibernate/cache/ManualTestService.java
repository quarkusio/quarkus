package org.infinispan.protean.hibernate.cache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ManualTestService implements TimeService {

    private final AtomicLong nanos = new AtomicLong();

    public void advance(long time, TimeUnit timeUnit) {
        final long nanoseconds = timeUnit.toNanos(time);
        nanos.addAndGet(nanoseconds);
    }

    @Override
    public long time() {
        return nanos.get();
    }

}
