package io.quarkus.vertx.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Build item produced by extensions that need to register Vert.x context locals
 * before the Vert.x instance is created.
 * <p>
 * This build item is intended to be produced by extensions and consumed by the
 * Vert.x Core extension, which executes all registrations before initializing Vert.x.
 */
public final class ContextLocalRegistrationBuildItem extends MultiBuildItem {

    private final RuntimeValue<Runnable> registration;

    public ContextLocalRegistrationBuildItem(RuntimeValue<Runnable> registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must not be null");
        }
        this.registration = registration;
    }

    public RuntimeValue<Runnable> getRegistration() {
        return registration;
    }
}
