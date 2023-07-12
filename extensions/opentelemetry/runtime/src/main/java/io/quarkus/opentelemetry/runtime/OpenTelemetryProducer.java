package io.quarkus.opentelemetry.runtime;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.DefaultBean;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerUtil;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.NameIterator;
import io.smallrye.config.SmallRyeConfig;

@Singleton
public class OpenTelemetryProducer {

    @Inject
    Instance<IdGenerator> idGenerator;
    @Inject
    @Any
    Instance<Resource> resources;
    @Inject
    @Any
    Instance<DelayedAttributes> delayedAttributes;
    @Inject
    @Any
    Instance<Sampler> sampler;
    @Inject
    @Any
    Instance<SpanProcessor> spanProcessors;
    @Inject
    OTelBuildConfig oTelBuildConfig;
    @Inject
    OTelRuntimeConfig oTelRuntimeConfig;

    @Inject
    ApplicationConfig appConfig;

    @Produces
    @Singleton
    @DefaultBean
    public OpenTelemetry getOpenTelemetry() {
        final Map<String, String> oTelConfigs = getOtelConfigs();

        if (oTelRuntimeConfig.sdkDisabled()) {
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
                        if (oTelBuildConfig.traces().enabled().orElse(TRUE)) {
                            Resource consolidatedResource = existingResource.merge(
                                    Resource.create(delayedAttributes.get())); // from cdi

                            // if user explicitly set 'otel.service.name', make sure we don't override it with defaults
                            // inside resource customizer
                            String serviceName = oTelRuntimeConfig
                                    .serviceName()
                                    .filter(sn -> !sn.equals(appConfig.name.orElse("unset")))
                                    .orElse(null);

                            // Merge resource instances with env attributes
                            Resource resource = resources.stream()
                                    .reduce(Resource.empty(), Resource::merge)
                                    .merge(TracerUtil.mapResourceAttributes(
                                            oTelRuntimeConfig.resourceAttributes().orElse(emptyList()),
                                            serviceName)); // from properties
                            return consolidatedResource.merge(resource);
                        } else {
                            return Resource.builder().build();
                        }
                    }
                })
                .addSamplerCustomizer(new BiFunction<Sampler, ConfigProperties, Sampler>() {
                    @Override
                    public Sampler apply(Sampler existingSampler, ConfigProperties configProperties) {
                        if (oTelBuildConfig.traces().enabled().orElse(TRUE)) {
                            final Sampler effectiveSampler = sampler.stream().findFirst()
                                    .map(Sampler.class::cast)// use CDI if it exists
                                    .orElse(existingSampler);

                            //collect default filtering targets (Needed for all samplers)
                            List<String> dropTargets = new ArrayList<>();
                            if (oTelRuntimeConfig.traces().suppressNonApplicationUris()) {//default is true
                                dropTargets.addAll(TracerRecorder.dropNonApplicationUriTargets);
                            }
                            if (!oTelRuntimeConfig.traces().includeStaticResources()) {// default is false
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
                                if (oTelBuildConfig.traces().enabled().orElse(TRUE)) {
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
