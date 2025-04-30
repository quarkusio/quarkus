package io.quarkus.arc.impl;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.arc.ContextInstanceHandle;

public interface ContextInstances {

    /**
     *
     * @param id
     * @param supplier
     * @return the instance handle
     */
    ContextInstanceHandle<?> computeIfAbsent(String id, Supplier<ContextInstanceHandle<?>> supplier);

    /**
     *
     * @param id
     * @return the instance handle if present, {@code null} otherwise
     */
    ContextInstanceHandle<?> getIfPresent(String id);

    /**
     *
     * @param id
     * @return the removed instance handle, or {@code null}
     */
    ContextInstanceHandle<?> remove(String id);

    /**
     *
     * @return all instance handles
     */
    Set<ContextInstanceHandle<?>> getAllPresent();

    /**
     * Removes all instance handles and performs the given action (if present) for each handle.
     *
     * @param action
     */
    void removeEach(Consumer<? super ContextInstanceHandle<?>> action);

}
