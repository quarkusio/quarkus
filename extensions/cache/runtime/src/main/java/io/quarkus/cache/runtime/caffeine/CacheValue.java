package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;

public class CacheValue<T> {

    CacheValue(T data, Duration expiresIn) {
        this.data = data;
        this.expiresIn = expiresIn;
    }

    public static <T> CacheValueBuilder<T> builder() {
        return new CacheValueBuilder<>();
    }

    public static class CacheValueBuilder<T> {

        Duration expiresIn;
        T data;

        public CacheValueBuilder<T> expiresIn(Duration expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public CacheValueBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public CacheValue<T> build() {
            return new CacheValue<>(data, expiresIn);
        }
    }

    final Duration expiresIn;
    final T data;

    public T getData() {
        return data;
    }

}
