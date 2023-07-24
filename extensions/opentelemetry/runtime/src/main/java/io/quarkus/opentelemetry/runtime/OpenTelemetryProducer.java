package io.quarkus.opentelemetry.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.NameIterator;
import io.smallrye.config.SmallRyeConfig;

@Singleton
public class OpenTelemetryProducer {

    public void disposeOfOpenTelemetry(@Disposes OpenTelemetry openTelemetry) {
        if (openTelemetry instanceof OpenTelemetrySdk) {
            var openTelemetrySdk = ((OpenTelemetrySdk) openTelemetry);
            openTelemetrySdk.getSdkTracerProvider().forceFlush();
            openTelemetrySdk.getSdkTracerProvider().shutdown();
        }
    }

    @Produces
    @Singleton
    @DefaultBean
    public OpenTelemetry getOpenTelemetry(OTelRuntimeConfig oTelRuntimeConfig,
            @All List<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> builderCustomizers) {
        final Map<String, String> oTelConfigs = getOtelConfigs();

        if (oTelRuntimeConfig.sdkDisabled()) {
            return AutoConfiguredOpenTelemetrySdk.builder()
                    .setResultAsGlobal(true)
                    .registerShutdownHook(false)
                    .addPropertiesSupplier(() -> oTelConfigs)
                    .build()
                    .getOpenTelemetrySdk();
        }

        var builder = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal(true)
                .registerShutdownHook(false)
                .addPropertiesSupplier(() -> oTelConfigs)
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

        // load new properties
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("quarkus.otel.")) {
                ConfigValue configValue = config.getConfigValue(propertyName);
                if (configValue.getValue() != null) {
                    NameIterator name = new NameIterator(propertyName);
                    name.next();
                    oTelConfigs.put(name.getName().substring(name.getPosition() + 1), configValue.getValue());
                }
            }
        }
        return oTelConfigs;
    }
}
