package io.quarkus.vertx.http.deployment;

import java.util.function.BiConsumer;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

/**
 * This way, extensions may register their own default {@link QuarkusHttpUser#AUTH_FAILURE_HANDLER}.
 * If proactive auth is used, this is the only one.
 */
public final class DefaultAuthFailureHandlerBuildItem extends SimpleBuildItem {
    private final BiConsumer<RoutingContext, Throwable> defaultAuthFailureHandler;

    public DefaultAuthFailureHandlerBuildItem(BiConsumer<RoutingContext, Throwable> defaultAuthFailureHandler) {
        this.defaultAuthFailureHandler = defaultAuthFailureHandler;
    }

    public BiConsumer<RoutingContext, Throwable> getDefaultAuthFailureHandler() {
        return defaultAuthFailureHandler;
    }
}
