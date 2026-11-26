package io.quarkus.opentelemetry.runtime.tracing.instrumentation;

import java.util.function.Consumer;

import io.vertx.core.internal.VertxBootstrap;

/**
 * Customizes the Vert.x bootstrap: sets the tracer or the metrics factory of OpenTelemetry.
 * <p>
 * A service type cannot be generic, so this type names {@code Consumer<VertxBootstrap>} for the services that
 * {@code VertxBootstrapConsumerBuildItem} carries.
 */
@FunctionalInterface
public interface OTelVertxBootstrapCustomizer extends Consumer<VertxBootstrap> {
}
