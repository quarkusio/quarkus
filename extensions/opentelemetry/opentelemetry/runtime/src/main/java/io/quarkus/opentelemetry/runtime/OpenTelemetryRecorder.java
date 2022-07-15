package io.quarkus.opentelemetry.runtime;

import static java.lang.Boolean.TRUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.config.OtelBuildConfig;
import io.quarkus.opentelemetry.runtime.tracing.LateBoundSampler;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {

    //    public static final String QUARKUS_OTEL_PREFIX = "quarkus.otel.";
    public static final String QUARKUS_OTEL_PREFIX = "otel.";

    /* STATIC INIT */
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
    }

    /* STATIC INIT */
    public RuntimeValue<OpenTelemetry> createOpenTelemetry(
            OtelBuildConfig buildConfig,
            RuntimeValue<Optional<IdGenerator>> idGenerator,
            RuntimeValue<Optional<Resource>> resource,
            RuntimeValue<Optional<LateBoundSampler>> sampler,
            RuntimeValue<List<SpanExporter>> spanExporters, // SPI
            RuntimeValue<List<SpanProcessor>> spanProcessors,
            ShutdownContext shutdownContext) {

        Map<String, String> oTelConfigs = new HashMap<>();
        Config config = ConfigProvider.getConfig();
        for (String propertyName : config.getPropertyNames()) {
            // FIXME must be quarkus.otel.
            if (propertyName.startsWith(QUARKUS_OTEL_PREFIX) || propertyName.startsWith("OTEL_")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                        // TODO add property transformation
                        value -> oTelConfigs.put(propertyName, value));
            }
        }

        OpenTelemetrySdk openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal(true)
                .registerShutdownHook(false)
                .addPropertiesSupplier(() -> oTelConfigs)
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                // no customization needed for spanExporter. Loads SPI from CDI
                .addResourceCustomizer((existingResource, configProperties) -> {
                    if (buildConfig.traces().enabled().orElse(TRUE)) {
                        return resource.getValue()
                                .map(existingResource::merge)
                                .orElse(existingResource);
                    } else {
                        return Resource.builder().build();
                    }
                })
                .addSamplerCustomizer(new BiFunction<Sampler, ConfigProperties, Sampler>() {
                    @Override
                    public Sampler apply(Sampler existingSampler, ConfigProperties configProperties) {
                        if (buildConfig.traces().enabled().orElse(TRUE)) {
                            return sampler.getValue()
                                    .map(lateBoundSampler -> (Sampler) lateBoundSampler)// use CDI if it exists
                                    .orElse(existingSampler);
                        } else {
                            return Sampler.alwaysOff();
                        }
                    }
                })
                .addTracerProviderCustomizer((builder, configProperties) -> {
                    if (buildConfig.traces().enabled().orElse(TRUE)) {
                        idGenerator.getValue().ifPresent(builder::setIdGenerator);
                        spanProcessors.getValue().forEach(spanProcessor -> builder.addSpanProcessor(spanProcessor));
                    }
                    return builder;
                })
                .build()
                .getOpenTelemetrySdk();

        // Register shutdown tasks, because we are using CDI beans
        shutdownContext.addShutdownTask(() -> {
            openTelemetrySdk.getSdkTracerProvider().forceFlush();
            openTelemetrySdk.getSdkTracerProvider().shutdown();
        });

        return new RuntimeValue<>(openTelemetrySdk);
    }

    /* STATIC INIT */
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    /* RUNTIME INIT */
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }
}
