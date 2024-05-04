package io.quarkus.opentelemetry.deployment.tracing;

import java.util.function.BooleanSupplier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

public class MongoTracingEnabled implements BooleanSupplier {

    private static final String MONGO_TRACER_CONFIG = "io.quarkus.mongodb.runtime.MongodbConfig";

    public boolean getAsBoolean() {
        return QuarkusClassLoader.isClassPresentAtRuntime(MONGO_TRACER_CONFIG);
    }
}
