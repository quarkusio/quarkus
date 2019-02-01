package org.infinispan.protean.hibernate.cache;

import java.time.Duration;

final class InternalCacheConfig {

    long maxSize = -1;
    Duration maxIdle; // in seconds

    void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    void setMaxIdle(Duration maxIdle) {
        this.maxIdle = maxIdle;
    }

}
