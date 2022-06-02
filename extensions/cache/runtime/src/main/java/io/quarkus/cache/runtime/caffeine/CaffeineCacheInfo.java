package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.Objects;

public class CaffeineCacheInfo {

    public String name;

    public Integer initialCapacity;

    public Long maximumSize;

    public Duration expireAfterWrite;

    public Duration expireAfterAccess;

    public Duration refreshAfterWrite;

    public boolean metricsEnabled;

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CaffeineCacheInfo) {
            CaffeineCacheInfo other = (CaffeineCacheInfo) obj;
            return Objects.equals(name, other.name);
        }
        return false;
    }
}
