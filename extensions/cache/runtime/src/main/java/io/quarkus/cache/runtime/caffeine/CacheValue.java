package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;

record CacheValue<T>(T data, Duration expiresAfter) {
}
