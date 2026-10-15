package io.quarkus.vertx.core.deployment;

import java.util.List;
import java.util.Objects;

import org.jboss.threads.ContextHandler;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * A build item that allows extensions to declare which keys should not be propagated before a task is executed in
 * the {@link ContextHandler#runWith(Runnable, Object)} method.
 */
public final class IgnoredContextLocalDataKeysBuildItem extends MultiBuildItem {

    private final List<String> ignoredKeys;

    /**
     * Construct a new instance with a plain list of ignored keys.
     *
     * @param ignoredKeys the list of context local data keys to ignore (must not be {@code null})
     */
    public IgnoredContextLocalDataKeysBuildItem(List<String> ignoredKeys) {
        this.ignoredKeys = List.copyOf(Objects.requireNonNull(ignoredKeys));
    }

    /**
     * Construct a new instance from a {@link RuntimeValue} containing the ignored keys.
     *
     * @param ignoredKeysSupplier a runtime value wrapping the list of keys (must not be {@code null})
     * @deprecated Use {@link #IgnoredContextLocalDataKeysBuildItem(List)} instead.
     */
    @Deprecated(forRemoval = true)
    public IgnoredContextLocalDataKeysBuildItem(RuntimeValue<List<String>> ignoredKeysSupplier) {
        this(Objects.requireNonNull(ignoredKeysSupplier).getValue());
    }

    /**
     * Get the list of context local data keys that should not be propagated.
     *
     * @return the ignored keys (not {@code null})
     */
    public List<String> ignoredKeys() {
        return ignoredKeys;
    }
}
