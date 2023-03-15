package io.quarkus.opentelemetry.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.BeanManager;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.events.GlobalEventEmitterProvider;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {

    public static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";

    /* STATIC INIT */
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
        GlobalLoggerProvider.resetForTest();
        GlobalEventEmitterProvider.resetForTest();
    }

    /* RUNTIME INIT */
    public RuntimeValue<OpenTelemetry> createOpenTelemetry(ShutdownContext shutdownContext) {

        BeanManager beanManager = Arc.container().beanManager();

        OpenTelemetry openTelemetry = beanManager.createInstance()
                .select(OpenTelemetry.class, Any.Literal.INSTANCE).get();

        // Because we are producing the ObfuscatedOpenTelemetry. These methods are not be available otherwise.
        // Register shutdown tasks, because we are using CDI beans
        shutdownContext.addShutdownTask(() -> {
            ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
            ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().shutdown();
        });

        return new RuntimeValue<>(openTelemetry);
    }

    /* RUNTIME INIT */
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    /* RUNTIME INIT */
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }

}
