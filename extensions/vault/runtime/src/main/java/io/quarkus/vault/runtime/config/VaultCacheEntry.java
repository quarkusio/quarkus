package io.quarkus.vault.runtime.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.jboss.logging.Logger;

public class VaultCacheEntry<V> {

    private static final Logger log = Logger.getLogger(VaultCacheEntry.class.getName());

    private V value;
    private volatile Instant created = Instant.now();

    public static <V> V tryReturnLastKnownValue(RuntimeException e, VaultCacheEntry<V> cacheEntry) {
        if (e.getCause() instanceof IOException && cacheEntry != null) {
            log.debug("unable to fetch secrets from vault; returning last known value", e);
            cacheEntry.reset(); // will return values from the cache for the cache-period
            return cacheEntry.getValue();
        } else {
            throw e;
        }
    }

    public VaultCacheEntry(V value) {
        this.value = value;
    }

    public boolean youngerThan(Duration duration) {
        return created.plus(duration).isAfter(Instant.now());
    }

    public V getValue() {
        return value;
    }

    public void reset() {
        created = Instant.now();
    }

}
