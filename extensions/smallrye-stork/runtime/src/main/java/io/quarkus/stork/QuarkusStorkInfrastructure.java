package io.quarkus.stork;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.smallrye.stork.integration.DefaultStorkInfrastructure;
import io.vertx.core.Vertx;

@Singleton
public class QuarkusStorkInfrastructure extends DefaultStorkInfrastructure {
    private final Vertx vertx;

    public QuarkusStorkInfrastructure(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public <T> T get(Class<T> utilityClass, Supplier<T> defaultSupplier) {
        if (utilityClass.isInstance(vertx)) {
            //noinspection unchecked
            return (T) vertx;
        }
        return super.get(utilityClass, defaultSupplier);
    }
}
