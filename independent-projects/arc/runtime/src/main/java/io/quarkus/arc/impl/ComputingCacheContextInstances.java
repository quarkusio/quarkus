package io.quarkus.arc.impl;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.arc.ContextInstanceHandle;

class ComputingCacheContextInstances implements ContextInstances {

    protected final ComputingCache<String, ContextInstanceHandle<?>> instances;

    ComputingCacheContextInstances() {
        instances = new ComputingCache<>();
    }

    @Override
    public ContextInstanceHandle<?> computeIfAbsent(String id, Supplier<ContextInstanceHandle<?>> supplier) {
        return instances.computeIfAbsent(id, supplier);
    }

    @Override
    public ContextInstanceHandle<?> getIfPresent(String id) {
        return instances.getValueIfPresent(id);
    }

    @Override
    public ContextInstanceHandle<?> remove(String id) {
        return instances.remove(id);
    }

    @Override
    public Set<ContextInstanceHandle<?>> getAllPresent() {
        return instances.getPresentValues();
    }

    @Override
    public void removeEach(Consumer<? super ContextInstanceHandle<?>> action) {
        if (action != null) {
            instances.getPresentValues().forEach(action);
        }
        instances.clear();
    }

}
