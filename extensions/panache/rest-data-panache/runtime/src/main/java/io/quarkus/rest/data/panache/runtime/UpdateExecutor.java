package io.quarkus.rest.data.panache.runtime;

import java.util.function.Supplier;

public interface UpdateExecutor {
    <T> T execute(Supplier<T> supplier);
}
