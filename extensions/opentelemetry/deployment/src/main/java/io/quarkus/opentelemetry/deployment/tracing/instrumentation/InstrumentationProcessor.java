package io.quarkus.opentelemetry.deployment.tracing.instrumentation;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isClassPresentAtRuntime;
import static jakarta.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerEnabled;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.InstrumentationRecorder;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc.GrpcTracingClientInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.grpc.GrpcTracingServerInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging.ReactiveMessagingTracingIncomingDecorator;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.reactivemessaging.ReactiveMessagingTracingOutgoingDecorator;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.restclient.OpenTelemetryClientFilter;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy.AttachExceptionHandler;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy.OpenTelemetryClassicServerFilter;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy.OpenTelemetryReactiveServerFilter;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.PreExceptionMapperHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import io.quarkus.vertx.core.deployment.VertxOptionsConsumerBuildItem;
import io.vertx.core.VertxOptions;

@BuildSteps(onlyIf = TracerEnabled.class)
public class InstrumentationProcessor {
    static class MetricsExtensionAvailable implements BooleanSupplier {
        private static final boolean IS_MICROMETER_EXTENSION_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.micrometer.runtime.binder.vertx.VertxHttpServerMetrics");

        @Override
        public boolean getAsBoolean() {
            Config config = ConfigProvider.getConfig();
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
                && config.instrument().restClientClassic()) {
            additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(OpenTelemetryClientFilter.class.getName()));
            additionalBeans.produce(new AdditionalBeanBuildItem(OpenTelemetryClientFilter.class));
        }
    }

    @BuildStep
    void registerReactiveMessagingMessageDecorator(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            OTelBuildConfig config) {
        if (capabilities.isPresent(Capability.SMALLRYE_REACTIVE_MESSAGING) && config.instrument().reactiveMessaging()) {
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveMessagingTracingOutgoingDecorator.class));
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveMessagingTracingIncomingDecorator.class));
        }
    }

    @BuildStep(onlyIfNot = MetricsExtensionAvailable.class)
    @Record(ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem vertxTracingMetricsOptions(InstrumentationRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.getVertxTracingMetricsOptions(), LIBRARY_AFTER + 1);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxOptionsConsumerBuildItem vertxTracingOptions(
            InstrumentationRecorder recorder) {
        Consumer<VertxOptions> vertxTracingOptions;
        vertxTracingOptions = recorder.getVertxTracingOptions();
        return new VertxOptionsConsumerBuildItem(vertxTracingOptions, LIBRARY_AFTER);
    }

    // RESTEasy and Vert.x web
    @BuildStep
    void registerResteasyClassicAndOrResteasyReactiveProvider(OTelBuildConfig config,
            Capabilities capabilities,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviderBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.RESTEASY) && config.instrument().resteasyClassic()) {
            resteasyJaxrsProviderBuildItemBuildProducer
                    .produce(new ResteasyJaxrsProviderBuildItem(OpenTelemetryClassicServerFilter.class.getName()));
        }
    }

    @BuildStep
    void resteasyReactiveIntegration(
            Capabilities capabilities,
            BuildProducer<CustomContainerRequestFilterBuildItem> containerRequestFilterBuildItemBuildProducer,
            BuildProducer<PreExceptionMapperHandlerBuildItem> preExceptionMapperHandlerBuildItemBuildProducer,
            OTelBuildConfig config) {
        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE) && config.instrument().resteasyReactive()) {
            containerRequestFilterBuildItemBuildProducer
                    .produce(new CustomContainerRequestFilterBuildItem(OpenTelemetryReactiveServerFilter.class.getName()));
            preExceptionMapperHandlerBuildItemBuildProducer
                    .produce(new PreExceptionMapperHandlerBuildItem(new AttachExceptionHandler()));
        }

    }
}
