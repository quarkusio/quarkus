package io.quarkus.vertx.http.deployment;

import java.util.Objects;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * This is a {@code MultiBuildItem} so that multiple producers may exist
 * among the set of currently present extensions. However, at most one item
 * of this type may be produced.
 */
public final class SessionStoreProviderBuildItem extends MultiBuildItem {
    private final Supplier<SessionStore> provider;

    public SessionStoreProviderBuildItem(Supplier<SessionStore> provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    public Supplier<SessionStore> getProvider() {
        return provider;
    }
}
