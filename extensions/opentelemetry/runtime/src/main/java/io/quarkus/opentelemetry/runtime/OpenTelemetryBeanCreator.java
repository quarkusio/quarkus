package io.quarkus.opentelemetry.runtime;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.spi.Converter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigValue;

/**
 * Creates the default {@link OpenTelemetry} bean, and sets it as the global {@link OpenTelemetry}.
 */
public class OpenTelemetryBeanCreator implements BeanCreator<OpenTelemetry> {

    @SuppressWarnings("removal")
    @Override
    public OpenTelemetry create(SyntheticCreationalContext<OpenTelemetry> context) {
        OpenTelemetryUtil.checkNotStaticInit(OpenTelemetry.class);
        OTelRuntimeConfig runtimeConfig = Config.get().getConfigMapping(OTelRuntimeConfig.class);
        Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> builderCustomizers = context
                .getInjectedReference(new TypeLiteral<>() {
                });

        if (LaunchMode.current().isDevOrTest()) {
            // The global OpenTelemetry of the previous application must be gone before this one is set as global
            GlobalOpenTelemetry.resetForTest();
        }

        final Map<String, String> oTelConfigs = getOtelConfigs(runtimeConfig);
        OtelConfigsSupplier propertiesSupplier = new OtelConfigsSupplier(oTelConfigs);
        if (runtimeConfig.sdkDisabled()) {
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

    private static Map<String, String> getOtelConfigs(OTelRuntimeConfig runtimeConfig) {
        Map<String, String> oTelConfigs = new HashMap<>();
        Config config = Config.get();

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

        if (runtimeConfig.mpCompatibility()) {
            oTelConfigs.putAll(quarkus);
            oTelConfigs.putAll(otel);
        } else {
            oTelConfigs.putAll(otel);
            oTelConfigs.putAll(quarkus);
        }

        ResourceConfigReconciler.reconcile(oTelConfigs,
                runtimeConfig.resourceAttributes().orElse(List.of()));

        return oTelConfigs;
    }

    /**
     * Transforms the value to what OTel expects
     * TODO: this is super simplistic, and should be more modular if needed
     */
    private static class OTelDurationConverter implements Converter<String> {
        static OTelDurationConverter INSTANCE = new OTelDurationConverter();

        private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");

        @Override
        public String convert(final String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                throw new NullPointerException();
            }

            if (DIGITS.asPredicate().test(value)) {
                // OTel regards values without a unit to be milliseconds instead of seconds
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
