package io.quarkus.opentelemetry.runtime;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.All;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.RemoveableLateBoundBatchSpanProcessor;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.opentelemetry.runtime.tracing.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerUtil;
import io.quarkus.runtime.ApplicationConfig;

public interface AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    void customize(AutoConfiguredOpenTelemetrySdkBuilder builder);

    @Singleton
    final class TracingResourceCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

        private final ApplicationConfig appConfig;
        private final OTelBuildConfig oTelBuildConfig;
        private final OTelRuntimeConfig oTelRuntimeConfig;
        private final Instance<DelayedAttributes> delayedAttributes;
        private final List<Resource> resources;

        public TracingResourceCustomizer(ApplicationConfig appConfig,
                OTelBuildConfig oTelBuildConfig,
                OTelRuntimeConfig oTelRuntimeConfig,
                @Any Instance<DelayedAttributes> delayedAttributes,
                @All List<Resource> resources) {
            this.appConfig = appConfig;
            this.oTelBuildConfig = oTelBuildConfig;
            this.oTelRuntimeConfig = oTelRuntimeConfig;
            this.delayedAttributes = delayedAttributes;
            this.resources = resources;
        }

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            builder.addResourceCustomizer(new BiFunction<>() {
                @Override
                public Resource apply(Resource existingResource, ConfigProperties configProperties) {
                    if (oTelBuildConfig.traces().enabled().orElse(TRUE) ||
                            oTelBuildConfig.metrics().enabled().orElse(TRUE)) {
                        Resource consolidatedResource = existingResource.merge(
                                Resource.create(delayedAttributes.get()));

                        // if user explicitly set 'otel.service.name', make sure we don't override it with defaults
                        // inside resource customizer
                        String serviceName = oTelRuntimeConfig
                                .serviceName()
                                .filter(new Predicate<String>() {
                                    @Override
                                    public boolean test(String sn) {
                                        return !sn.equals(appConfig.name.orElse("unset"));
                                    }
                                })
                                .orElse(null);

                        String hostname = getHostname();

                        // Merge resource instances with env attributes
                        Resource resource = resources.stream()
                                .reduce(Resource.empty(), Resource::merge)
                                .merge(TracerUtil.mapResourceAttributes(
                                        oTelRuntimeConfig.resourceAttributes().orElse(emptyList()),
                                        serviceName, // from properties
                                        hostname));
                        return consolidatedResource.merge(resource);
                    } else {
                        return Resource.builder().build();
                    }
                }
            });
        }
    }

    @Singleton
    final class SamplerCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
        private final OTelBuildConfig oTelBuildConfig;
        private final OTelRuntimeConfig oTelRuntimeConfig;
        private final List<Sampler> sampler;

        public SamplerCustomizer(OTelBuildConfig oTelBuildConfig,
                OTelRuntimeConfig oTelRuntimeConfig,
                @All List<Sampler> sampler) {
            this.oTelBuildConfig = oTelBuildConfig;
            this.oTelRuntimeConfig = oTelRuntimeConfig;
            this.sampler = sampler;
        }

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            builder.addSamplerCustomizer(new BiFunction<>() {
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
            });
        }
    }

    @Singleton
    final class TracerProviderCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

        private final OTelBuildConfig oTelBuildConfig;
        private final List<IdGenerator> idGenerator;
        private final List<SpanProcessor> spanProcessors;

        public TracerProviderCustomizer(OTelBuildConfig oTelBuildConfig,
                @All List<IdGenerator> idGenerator,
                @All List<SpanProcessor> spanProcessors) {
            this.oTelBuildConfig = oTelBuildConfig;
            this.idGenerator = idGenerator;
            this.spanProcessors = spanProcessors;
        }

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            builder.addTracerProviderCustomizer(
                    new BiFunction<>() {
                        @Override
                        public SdkTracerProviderBuilder apply(SdkTracerProviderBuilder tracerProviderBuilder,
                                ConfigProperties configProperties) {
                            if (oTelBuildConfig.traces().enabled().orElse(TRUE)) {
                                idGenerator.stream().findFirst().ifPresent(tracerProviderBuilder::setIdGenerator); // from cdi
                                spanProcessors.stream().filter(new Predicate<SpanProcessor>() {
                                    @Override
                                    public boolean test(SpanProcessor sp) {
                                        return !(sp instanceof RemoveableLateBoundBatchSpanProcessor);
                                    }
                                })
                                        .forEach(tracerProviderBuilder::addSpanProcessor);
                            }
                            return tracerProviderBuilder;
                        }
                    });
        }
    }

    @Singleton
    final class MetricProviderCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
        private final OTelBuildConfig oTelBuildConfig;
        private final Instance<Clock> clock;

        public MetricProviderCustomizer(OTelBuildConfig oTelBuildConfig,
                final Instance<Clock> clock) {
            this.oTelBuildConfig = oTelBuildConfig;
            this.clock = clock;
        }

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            if (oTelBuildConfig.metrics().enabled().orElse(TRUE)) {
                builder.addMeterProviderCustomizer(
                        new BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder>() {
                            @Override
                            public SdkMeterProviderBuilder apply(SdkMeterProviderBuilder metricProvider,
                                    ConfigProperties configProperties) {
                                if (clock.isUnsatisfied()) {
                                    throw new IllegalStateException("No Clock bean found");
                                }
                                metricProvider.setClock(clock.get());
                                return metricProvider;
                            }
                        });
            }
        }
    }

    @Singleton
    final class TextMapPropagatorCustomizers implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

        private final List<TextMapPropagatorCustomizer> customizers;

        public TextMapPropagatorCustomizers(@All List<TextMapPropagatorCustomizer> customizers) {
            this.customizers = customizers;
        }

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            for (TextMapPropagatorCustomizer customizer : customizers) {
                builder.addPropagatorCustomizer(
                        new BiFunction<>() {
                            @Override
                            public TextMapPropagator apply(TextMapPropagator textMapPropagator,
                                    ConfigProperties configProperties) {
                                return customizer.customize(new TextMapPropagatorCustomizer.Context() {
                                    @Override
                                    public TextMapPropagator propagator() {
                                        return textMapPropagator;
                                    }

                                    @Override
                                    public ConfigProperties otelConfigProperties() {
                                        return configProperties;
                                    }
                                });
                            }
                        });
            }
        }
    }

    private static String getHostname() {
        // must be resolved at startup, once.
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return hostname;
    }

}
