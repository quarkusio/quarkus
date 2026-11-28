package io.quarkus.opentelemetry.deployment.tracing.instrumentation;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isClassPresentAtRuntime;
import static jakarta.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerEnabled;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.OTelVertxBootstrapCustomizer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.OpenTelemetryVertxInstrumentation;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.grpc.GrpcTracingClientInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.grpc.GrpcTracingServerInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.reactivemessaging.ReactiveMessagingTracingIncomingDecorator;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.reactivemessaging.ReactiveMessagingTracingOutgoingDecorator;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.restclient.OpenTelemetryClientFilter;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.restclient.OpenTelemetryReactiveClientContextFilter;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.resteasy.AttachExceptionHandler;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.resteasy.OpenTelemetryClassicServerFilter;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.resteasy.OpenTelemetryReactiveServerFilter;
import io.quarkus.rest.client.reactive.spi.ClientRequestFilterBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.KnownServerRestHandlerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.PreExceptionMapperHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import io.quarkus.vertx.deployment.spi.VertxBootstrapConsumerBuildItem;
import io.smallrye.config.Config;

@BuildSteps(onlyIf = TracerEnabled.class)
public class InstrumentationProcessor {

    private static final String VERTX_HTTP_METRICS = "opentelemetry.vertx.http-metrics";
    private static final String VERTX_METRICS = "opentelemetry.vertx.metrics";
    private static final String VERTX_TRACING = "opentelemetry.vertx.tracing";

    static class MetricsExtensionAvailable implements BooleanSupplier {
        private static final boolean IS_MICROMETER_EXTENSION_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.micrometer.runtime.binder.vertx.VertxHttpServerMetrics");

        @Override
        public boolean getAsBoolean() {
            Config config = Config.get();
            if (IS_MICROMETER_EXTENSION_AVAILABLE) {
                if (config.getOptionalValue("quarkus.micrometer.enabled", Boolean.class).orElse(true)) {
                    Optional<Boolean> httpServerEnabled = config
                            .getOptionalValue("quarkus.micrometer.binder.http-server.enabled", Boolean.class);
                    if (httpServerEnabled.isPresent()) {
                        return httpServerEnabled.get();
                    } else {
                        return config.getOptionalValue("quarkus.micrometer.binder-enabled-default", Boolean.class).orElse(true);
                    }
                }
            }
            return false;
        }
    }

    static class GrpcExtensionAvailable implements BooleanSupplier {
        private static final boolean IS_GRPC_EXTENSION_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.grpc.runtime.GrpcServerRecorder");

        @Override
        public boolean getAsBoolean() {
            return IS_GRPC_EXTENSION_AVAILABLE;
        }
    }

    static class VertxHttpAvailable implements BooleanSupplier {
        private static final boolean IS_VERTX_HTTP_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.vertx.http.runtime.VertxHttpRecorder");

        @Override
        public boolean getAsBoolean() {
            return IS_VERTX_HTTP_AVAILABLE;
        }
    }

    @BuildStep(onlyIf = GrpcExtensionAvailable.class)
    void grpcTracers(BuildProducer<AdditionalBeanBuildItem> additionalBeans, OTelBuildConfig config) {
        if (config.instrument().grpc()) {
            additionalBeans.produce(new AdditionalBeanBuildItem(GrpcTracingServerInterceptor.class));
            additionalBeans.produce(new AdditionalBeanBuildItem(GrpcTracingClientInterceptor.class));
        }
    }

    @BuildStep
    void registerRestClientClassicProvider(
            Capabilities capabilities,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            OTelBuildConfig config) {
        if (capabilities.isPresent(Capability.REST_CLIENT) && capabilities.isMissing(Capability.REST_CLIENT_REACTIVE)
                && config.instrument().resteasyClient()) {
            additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(OpenTelemetryClientFilter.class.getName()));
            additionalBeans.produce(new AdditionalBeanBuildItem(OpenTelemetryClientFilter.class));
        }
    }

    /**
     * With the Quarkus REST Client the span is created by the Vert.x tracer: this request filter only hands the caller's
     * OpenTelemetry context over to the Vert.x context of the invocation.
     */
    @BuildStep
    void registerRestClientReactiveContextFilter(
            Capabilities capabilities,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<ClientRequestFilterBuildItem> clientRequestFilters) {
        if (capabilities.isPresent(Capability.REST_CLIENT_REACTIVE)) {
            additionalIndexed.produce(
                    new AdditionalIndexedClassesBuildItem(OpenTelemetryReactiveClientContextFilter.class.getName()));
            clientRequestFilters
                    .produce(new ClientRequestFilterBuildItem(OpenTelemetryReactiveClientContextFilter.class.getName()));
        }
    }

    @BuildStep
    void registerReactiveMessagingMessageDecorator(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            OTelBuildConfig config) {
        if (capabilities.isPresent(Capability.MESSAGING) && config.instrument().messaging()) {
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveMessagingTracingOutgoingDecorator.class));
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveMessagingTracingIncomingDecorator.class));
        }
    }

    // VertxBootstrapConsumerBuildItem carries the consumers that Vert.x core applies when it creates Vert.x, so each
    // consumer is a service bridged into the build item by a recorder proxy, until Vert.x core takes them as services

    @SuppressWarnings("deprecation")
    @BuildStep(onlyIfNot = MetricsExtensionAvailable.class, onlyIf = VertxHttpAvailable.class)
    VertxBootstrapConsumerBuildItem vertxHttpMetrics(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService(OTelVertxBootstrapCustomizer.class, VERTX_HTTP_METRICS)
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> OpenTelemetryVertxInstrumentation.httpMetrics());
        return new VertxBootstrapConsumerBuildItem(
                serviceRegistrar.getRecorderProxy(OTelVertxBootstrapCustomizer.class, VERTX_HTTP_METRICS),
                LIBRARY_AFTER + 1, VERTX_HTTP_METRICS);
    }

    @SuppressWarnings("deprecation")
    @BuildStep(onlyIfNot = { MetricsExtensionAvailable.class, VertxHttpAvailable.class })
    VertxBootstrapConsumerBuildItem vertxMetricsOptions(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService(OTelVertxBootstrapCustomizer.class, VERTX_METRICS)
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> OpenTelemetryVertxInstrumentation.metrics());
        return new VertxBootstrapConsumerBuildItem(
                serviceRegistrar.getRecorderProxy(OTelVertxBootstrapCustomizer.class, VERTX_METRICS),
                LIBRARY_AFTER + 2, VERTX_METRICS);
    }

    @SuppressWarnings("deprecation")
    @BuildStep
    VertxBootstrapConsumerBuildItem vertxTracingOptions(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService(OTelVertxBootstrapCustomizer.class, VERTX_TRACING)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(OTelRuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> OpenTelemetryVertxInstrumentation.tracing(runtimeConfig));
        return new VertxBootstrapConsumerBuildItem(
                serviceRegistrar.getRecorderProxy(OTelVertxBootstrapCustomizer.class, VERTX_TRACING),
                LIBRARY_AFTER, VERTX_TRACING);
    }

    // RESTEasy and Vert.x web
    @BuildStep
    void registerResteasyClassicAndOrResteasyReactiveProvider(OTelBuildConfig config,
            Capabilities capabilities,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviderBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.RESTEASY) && config.instrument().resteasy()) {
            resteasyJaxrsProviderBuildItemBuildProducer
                    .produce(new ResteasyJaxrsProviderBuildItem(OpenTelemetryClassicServerFilter.class.getName()));
        }
    }

    @BuildStep
    void resteasyReactiveIntegration(
            Capabilities capabilities,
            BuildProducer<CustomContainerRequestFilterBuildItem> containerRequestFilterBuildItemBuildProducer,
            BuildProducer<PreExceptionMapperHandlerBuildItem> preExceptionMapperHandlerBuildItemBuildProducer,
            BuildProducer<KnownServerRestHandlerBuildItem> knownServerRestHandlerBuildItemBuildProducer,
            OTelBuildConfig config) {
        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE) && config.instrument().rest()) {
            containerRequestFilterBuildItemBuildProducer
                    .produce(new CustomContainerRequestFilterBuildItem(OpenTelemetryReactiveServerFilter.class.getName()));
            preExceptionMapperHandlerBuildItemBuildProducer
                    .produce(new PreExceptionMapperHandlerBuildItem(new AttachExceptionHandler()));
            knownServerRestHandlerBuildItemBuildProducer
                    .produce(new KnownServerRestHandlerBuildItem(AttachExceptionHandler.class.getName()));
        }

    }
}
