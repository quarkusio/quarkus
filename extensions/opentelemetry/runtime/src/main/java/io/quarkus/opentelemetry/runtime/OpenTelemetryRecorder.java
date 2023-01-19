package io.quarkus.opentelemetry.runtime;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.opentelemetry.runtime.config.OpenTelemetryConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {

    public static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";
    private static final Logger LOG = Logger.getLogger(OpenTelemetryRecorder.class);

    /* STATIC INIT */
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
    }

    /* STATIC INIT */
    public RuntimeValue<OpenTelemetry> createOpenTelemetry(RuntimeValue<SdkTracerProvider> tracerProvider,
            OpenTelemetryConfig openTelemetryConfig) {
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();

        // Set tracer provider if present
        if (tracerProvider != null) {
            builder.setTracerProvider(tracerProvider.getValue());
        }

        builder.setPropagators(OpenTelemetryUtil.mapPropagators(openTelemetryConfig.propagators));

        OpenTelemetry openTelemetry = builder.buildAndRegisterGlobal();
        return new RuntimeValue<>(openTelemetry);
    }

    /* STATIC INIT */
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    /* RUNTIME INIT */
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }

    public void registerJdbcDriver(String driverClass) {
        try {
            var constructors = Class
                    .forName(driverClass, true, Thread.currentThread().getContextClassLoader())
                    .getConstructors();
            if (constructors.length == 1) {
                // create driver
                Driver driver = ((Driver) constructors[0].newInstance());
                // register the driver with OpenTelemetryDriver
                Class
                        .forName(OPEN_TELEMETRY_DRIVER, true, Thread.currentThread().getContextClassLoader())
                        .getMethod("addDriverCandidate", Driver.class)
                        .invoke(null, driver);
            } else {
                // drivers should have default constructor
                LOG.warn(String.format(
                        "Class '%s' has more than one constructor and won't be registered as driver. JDBC instrumentation might not work properly in native mode.",
                        driverClass));
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException
                | ClassNotFoundException e) {
            LOG.warn(String.format(
                    "Failed to register '%s' driver. JDBC instrumentation might not work properly in native mode.",
                    driverClass));
        }
    }
}
