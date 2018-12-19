package org.infinispan.protean.hibernate.cache;

import com.github.benmanes.caffeine.cache.Ticker;

public class CaffeineTimeService implements TimeService {

    private final Ticker ticker = Ticker.systemTicker();

    @Override
    public long time() {
        return ticker.read();
    }

}
