package io.quarkus.opentelemetry.runtime;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryUtil.subStringAfter;
import static io.quarkus.opentelemetry.runtime.config.OtelConfigRelocateConfigSourceInterceptor.RELOCATIONS;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.DefaultBean;
import io.quarkus.opentelemetry.runtime.config.build.LegacySamplerNameConverter;
import io.quarkus.opentelemetry.runtime.tracing.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerUtil;

@Singleton
public class OpenTelemetryProducer {

    private static final String QUARKUS_OTEL_PREFIX_PROP = "quarkus.otel.";
    private static final String QUARKUS_OTEL_LEGACY_PREFIX_PROP = "quarkus.opentelemetry.";
    private static final String QUARKUS_OTEL_TRACES_SAMPLER_PROP_NAME = "quarkus.otel.traces.sampler";
    private static final String QUARKUS_ENV_PREFIX = "quarkus.";

    @Inject
    private Instance<IdGenerator> idGenerator;

    @Inject
    @Any
    private Instance<Resource> resources;

    @Inject
    @Any
    private Instance<DelayedAttributes> delayedAttributes;

    @Inject
    @Any
    private Instance<Sampler> sampler;

    @Inject
    @Any
    private Instance<SpanProcessor> spanProcessors;

    @Inject
    @ConfigProperty(name = "quarkus.otel.traces.enabled")
    Optional<Boolean> tracesEnabled;

    @Inject
    @ConfigProperty(name = "quarkus.otel.sdk.disabled")
    boolean sdkDisabled;

    @Inject
    @ConfigProperty(name = "quarkus.otel.resource.attributes")
    Optional<List<String>> resourceAttributes;

    @Inject
    @ConfigProperty(name = "quarkus.otel.traces.suppress-non-application-uris")
    boolean suppressNonApplicationUris;

    @Inject
    @ConfigProperty(name = "quarkus.otel.traces.include-static-resources")
    boolean includeStaticResources;

    @Produces
    @Singleton
    @DefaultBean
    public OpenTelemetry getOpenTelemetry() {
        final Map<String, String> oTelConfigs = getOtelConfigs();

        if (sdkDisabled) {
            return AutoConfiguredOpenTelemetrySdk.builder()
                    .setResultAsGlobal(true)
                    .registerShutdownHook(false)
                    .addPropertiesSupplier(() -> oTelConfigs)
                    .build()
                    .getOpenTelemetrySdk();
        }

        final AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal(true)
                .registerShutdownHook(false)
                .addPropertiesSupplier(() -> oTelConfigs)
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                // no customization needed for spanExporter. Loads SPI from CDI
                .addResourceCustomizer(new BiFunction<Resource, ConfigProperties, Resource>() {
                    @Override
                    public Resource apply(Resource existingResource, ConfigProperties configProperties) {
                        if (tracesEnabled.orElse(TRUE)) {
                            Resource consolidatedResource = existingResource.merge(
                                    Resource.create(delayedAttributes.get())); // from cdi
                            // Merge resource instances with env attributes
                            Resource resource = resources.stream()
                                    .reduce(Resource.empty(), Resource::merge)
                                    .merge(
                                            TracerUtil.mapResourceAttributes(resourceAttributes.orElse(emptyList()))); // from properties
                            return consolidatedResource.merge(resource);
                        } else {
                            return Resource.builder().build();
                        }
                    }
                })
                .addSamplerCustomizer(new BiFunction<Sampler, ConfigProperties, Sampler>() {
                    @Override
                    public Sampler apply(Sampler existingSampler, ConfigProperties configProperties) {
                        if (tracesEnabled.orElse(TRUE)) {
                            final Sampler effectiveSampler = sampler.stream().findFirst()
                                    .map(Sampler.class::cast)// use CDI if it exists
                                    .orElse(existingSampler);

                            //collect default filtering targets (Needed for all samplers)
                            List<String> dropTargets = new ArrayList<>();
                            if (suppressNonApplicationUris) {//default is true
                                dropTargets.addAll(TracerRecorder.dropNonApplicationUriTargets);
                            }
                            if (!includeStaticResources) {// default is false
                                dropTargets.addAll(TracerRecorder.dropStaticResourceTargets);
                            }

                            // make sure dropped targets are not sampled
                            if (!dropTargets.isEmpty()) {
                                return new DropTargetsSampler(effectiveSampler, dropTargets);
                            } else {
                                return effectiveSampler;
                            }
                        } else {
                            return Sampler.alwaysOff();
                        }
                    }
                })
                .addTracerProviderCustomizer(
                        new BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>() {
                            @Override
                            public SdkTracerProviderBuilder apply(SdkTracerProviderBuilder builder,
                                    ConfigProperties configProperties) {
                                if (tracesEnabled.orElse(TRUE)) {
                                    idGenerator.stream().findFirst().ifPresent(builder::setIdGenerator); // from cdi
                                    spanProcessors.stream().forEach(builder::addSpanProcessor);
                                }
                                return builder;
                            }
                        })
                .build();
        return autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    }

    private Map<String, String> getOtelConfigs() {
        final Map<String, String> oTelConfigs = new HashMap<>();
        final Config config = ConfigProvider.getConfig();

        // instruct OTel that we are using the AutoConfiguredOpenTelemetrySdk
        oTelConfigs.put("otel.java.global-autoconfigure.enabled", "true");

        // load new properties
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith(QUARKUS_OTEL_PREFIX_PROP)) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                        new Consumer<String>() {
                            @Override
                            public void accept(String value) {
                                if (propertyName.equals(QUARKUS_OTEL_TRACES_SAMPLER_PROP_NAME)) {
                                    value = (new LegacySamplerNameConverter()).convert(value);
                                }
                                oTelConfigs.put(transformPropertyName(propertyName), value);
                            }
                        });
            }
        }

        //load legacy properties
        for (String oldPropertyName : config.getPropertyNames()) {
            if (oldPropertyName.startsWith(QUARKUS_OTEL_LEGACY_PREFIX_PROP)) {
                String newPropertyName = RELOCATIONS.get(oldPropertyName);
                if (newPropertyName != null) {
                    config.getOptionalValue(oldPropertyName, String.class).ifPresent(
                            new Consumer<String>() {
                                @Override
                                public void accept(String value) {
                                    if (newPropertyName.equals(QUARKUS_OTEL_TRACES_SAMPLER_PROP_NAME)) {
                                        value = (new LegacySamplerNameConverter()).convert(value);
                                    }
                                    oTelConfigs.put(transformPropertyName(newPropertyName), value);
                                }
                            });
                }
            }
        }
        return oTelConfigs;
    }

    private String transformPropertyName(String propertyName) {
        return subStringAfter(propertyName, QUARKUS_ENV_PREFIX);
    }
}
