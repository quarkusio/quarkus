package io.quarkus.arc.impl;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.arc.ContextInstanceHandle;

public interface ContextInstances {

    ContextInstanceHandle<?> computeIfAbsent(String id, Supplier<ContextInstanceHandle<?>> supplier);

    ContextInstanceHandle<?> getIfPresent(String id);

    ContextInstanceHandle<?> remove(String id);

    Set<ContextInstanceHandle<?>> getAllPresent();

    void clear();

    void forEach(Consumer<? super ContextInstanceHandle<?>> handleConsumer);

}
