package io.quarkus.mongodb.rest.data.panache.runtime;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.rest.data.panache.runtime.UpdateExecutor;

@Singleton
public class NoopUpdateExecutor implements UpdateExecutor {

    @Override
    public <T> T execute(Supplier<T> supplier) {
        return supplier.get();
    }
}
