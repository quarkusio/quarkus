package io.quarkus.load.shedding.runtime;

import java.util.concurrent.atomic.LongAdder;

import jakarta.inject.Singleton;

@Singleton
public class LoadSheddingCounter {
    private final LongAdder shedRequests = new LongAdder();

    /**
     * Records that a request was rejected.
     */
    public void requestShed() {
        shedRequests.increment();
    }

    public long coundShedRequests() {
        return shedRequests.sum();
    }
}
