package io.quarkus.opentelemetry.runtime;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_NAME;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_VERSION;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.events.GlobalEventLoggerProvider;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {
    public static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";

    private final RuntimeValue<OTelRuntimeConfig> runtimeConfig;

    public OpenTelemetryRecorder(final RuntimeValue<OTelRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @StaticInit
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
        GlobalEventLoggerProvider.resetForTest();
    }

    @StaticInit
    public Supplier<DelayedAttributes> delayedAttributes(String quarkusVersion,
            String serviceName,
            String serviceVersion) {
        return new Supplier<>() {
            @Override
            public DelayedAttributes get() {
                var result = new DelayedAttributes();
                result.setAttributesDelegate(Resource.getDefault()
                        .merge(Resource.create(
                                Attributes.of(
                                        SERVICE_NAME, serviceName,
                                        SERVICE_VERSION, serviceVersion,
                                        WEBENGINE_NAME, "Quarkus",
                                        WEBENGINE_VERSION, quarkusVersion)))
                        .getAttributes());
                return result;
            }
        };
    }

    @RuntimeInit
    public RuntimeValue<Boolean> isOtelSdkEnabled() {
        return new RuntimeValue<>(!runtimeConfig.getValue().sdkDisabled());
    }

    @RuntimeInit
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    @RuntimeInit
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }

    @RuntimeInit
    public Function<SyntheticCreationalContext<OpenTelemetry>, OpenTelemetry> opentelemetryBean() {
        return new Function<>() {
            @Override
            public OpenTelemetry apply(SyntheticCreationalContext<OpenTelemetry> context) {
                Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> builderCustomizers = context
                        .getInjectedReference(new TypeLiteral<>() {
                        });

                final Map<String, String> oTelConfigs = getOtelConfigs();
                OtelConfigsSupplier propertiesSupplier = new OtelConfigsSupplier(oTelConfigs);
                if (runtimeConfig.getValue().sdkDisabled()) {
                    return AutoConfiguredOpenTelemetrySdk.builder()
                            .setResultAsGlobal()
                            .disableShutdownHook()
                            .addPropertiesSupplier(propertiesSupplier)
                            .build()
                            .getOpenTelemetrySdk();
                }

                var builder = AutoConfiguredOpenTelemetrySdk.builder()
                        .setResultAsGlobal()
                        .disableShutdownHook()
                        .addPropertiesSupplier(propertiesSupplier)
                        .setServiceClassLoader(Thread.currentThread().getContextClassLoader());
                for (var customizer : builderCustomizers) {
                    customizer.customize(builder);
                }

                return builder.build().getOpenTelemetrySdk();
            }

            private Map<String, String> getOtelConfigs() {
                Map<String, String> oTelConfigs = new HashMap<>();
                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

                // instruct OTel that we are using the AutoConfiguredOpenTelemetrySdk
                oTelConfigs.put("otel.java.global-autoconfigure.enabled", "true");

                Map<String, String> otel = new HashMap<>();
                Map<String, String> quarkus = new HashMap<>();
                // load new properties
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("quarkus.otel.")) {
                        ConfigValue value = config.getConfigValue(propertyName);
                        if (value.getValue() != null) {
                            if (propertyName.endsWith("timeout") || propertyName.endsWith("delay")) {
                                quarkus.put(propertyName.substring(8),
                                        OTelDurationConverter.INSTANCE.convert(value.getValue()));
                            } else {
                                quarkus.put(propertyName.substring(8), value.getValue());
                            }
                        } else if (value.getValue() == null && value.getRawValue() != null) {
                            config.getValue(propertyName, String.class);
                        }
                    } else if (propertyName.startsWith("otel.")) {
                        ConfigValue value = config.getConfigValue(propertyName);
                        if (value.getValue() != null) {
                            otel.put(propertyName, value.getValue());
                        }
                    }
                }

                if (runtimeConfig.getValue().mpCompatibility()) {
                    oTelConfigs.putAll(quarkus);
                    oTelConfigs.putAll(otel);
                } else {
                    oTelConfigs.putAll(otel);
                    oTelConfigs.putAll(quarkus);
                }

                return oTelConfigs;
            }
        };
    }

    /**
     * Transforms the value to what OTel expects
     * TODO: this is super simplistic, and should be more modular if needed
     */
    private static class OTelDurationConverter implements Converter<String> {
        static OTelDurationConverter INSTANCE = new OTelDurationConverter();

        @Override
        public String convert(final String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                throw new NullPointerException();
            }

            if (DurationConverter.DIGITS.asPredicate().test(value)) {
                // OTel regards values without a unit to me milliseconds instead of seconds
                // that java.time.Duration assumes, so let's just not do any conversion and let OTel handle with it
                return value;
            }
            Duration duration;
            try {
                duration = DurationConverter.parseDuration(value);
            } catch (Exception ignored) {
                // it's not a Duration, so we can't do much
                return value;
            }

            if (duration == null) {
                return value;
            }

            try {
                return String.valueOf(duration.toMillis()).concat("ms");
            } catch (Exception ignored) {
                return String.valueOf(duration.toSeconds()).concat("s");
            }
        }
    }

    private static class OtelConfigsSupplier implements Supplier<Map<String, String>> {
        private final Map<String, String> oTelConfigs;

        public OtelConfigsSupplier(Map<String, String> oTelConfigs) {
            this.oTelConfigs = oTelConfigs;
        }

        @Override
        public Map<String, String> get() {
            return oTelConfigs;
        }
    }
}
