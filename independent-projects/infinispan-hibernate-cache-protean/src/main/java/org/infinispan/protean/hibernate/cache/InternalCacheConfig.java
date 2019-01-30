package org.infinispan.protean.hibernate.cache;

final class InternalCacheConfig {

    long maxSize = -1;
    long maxIdle = -1;

    void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    void setMaxIdle(long maxIdle) {
        this.maxIdle = maxIdle;
    }

}
