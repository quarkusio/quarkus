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

    private final RuntimeValue<List<String>> ignoredKeysSupplier;

    public IgnoredContextLocalDataKeysBuildItem(RuntimeValue<List<String>> ignoredKeysSupplier) {
        this.ignoredKeysSupplier = Objects.requireNonNull(ignoredKeysSupplier);
    }

    RuntimeValue<List<String>> getIgnoredKeysSupplier() {
        return ignoredKeysSupplier;
    }
}
