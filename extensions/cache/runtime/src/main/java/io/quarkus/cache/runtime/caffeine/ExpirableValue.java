package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;

/**
 * Internal wrapper used when per-entry (variable) expiration is enabled.
 */
final class ExpirableValue {

    final Object data;
    final Duration expiresAfter;

    ExpirableValue(Object data, Duration expiresAfter) {
        this.data = data;
        this.expiresAfter = expiresAfter;
    }

    static Object wrap(Object data, Duration expiresAfter) {
        return new ExpirableValue(data, expiresAfter);
    }

    static Object unwrapData(Object value) {
        if (value instanceof ExpirableValue) {
            return ((ExpirableValue) value).data;
        }
        return value;
    }

    static Duration expiresAfterOf(Object value) {
        if (value instanceof ExpirableValue) {
            return ((ExpirableValue) value).expiresAfter;
        }
        return null;
    }
}
