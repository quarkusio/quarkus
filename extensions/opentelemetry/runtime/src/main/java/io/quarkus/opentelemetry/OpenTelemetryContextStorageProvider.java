package io.quarkus.opentelemetry;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;

public class OpenTelemetryContextStorageProvider implements ContextStorageProvider {
    @Override
    public ContextStorage get() {
        return QuarkusContextStorage.INSTANCE;
    }
}
