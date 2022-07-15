package io.quarkus.vertx;

import java.util.function.Consumer;

import io.vertx.core.VertxOptions;

/**
 * Allows customizing the {@link VertxOptions} used to create the managed {@link io.vertx.core.Vertx} instance.
 * <p>
 * Beans exposing this interface receive the {@link VertxOptions} computed from the application configuration, and
 * extensions customizing the options.
 */
public interface VertxOptionsCustomizer extends Consumer<VertxOptions> {

}
