package io.quarkus.opentelemetry.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerProviderBuildItem;
import io.quarkus.opentelemetry.runtime.OpenTelemetryConfig;
import io.quarkus.opentelemetry.runtime.OpenTelemetryProducer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithSpanInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.restclient.OpenTelemetryClientFilter;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

public class OpenTelemetryProcessor {
    static class RestClientAvailable implements BooleanSupplier {
        private static final boolean IS_REST_CLIENT_AVAILABLE = isClassPresent("javax.ws.rs.client.ClientRequestFilter");

        @Override
        public boolean getAsBoolean() {
            return IS_REST_CLIENT_AVAILABLE;
        }
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY);
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(OpenTelemetryProducer.class)
                .build();
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void registerOpenTelemetryContextStorage(
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.opentelemetry.context.ContextStorageProvider"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, QuarkusContextStorage.class));
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void registerWithSpan(
            BuildProducer<InterceptorBindingRegistrarBuildItem> interceptorBindingRegistrar,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        interceptorBindingRegistrar.produce(new InterceptorBindingRegistrarBuildItem(
                new InterceptorBindingRegistrar() {
                    @Override
                    public List<InterceptorBinding> getAdditionalBindings() {
                        return List.of(InterceptorBinding.of(WithSpan.class, Set.of("value", "kind")));
                    }
                }));

        additionalBeans.produce(new AdditionalBeanBuildItem(WithSpanInterceptor.class));
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void transformWithSpan(
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(transformationContext -> {
            AnnotationTarget target = transformationContext.getTarget();
            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                if (target.asClass().name().equals(DotName.createSimple(WithSpanInterceptor.class.getName()))) {
                    transformationContext.transform().add(DotName.createSimple(WithSpan.class.getName())).done();
                }
            }
        }));
    }

    @BuildStep(onlyIf = { OpenTelemetryEnabled.class, RestClientAvailable.class })
    void registerProvider(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(OpenTelemetryClientFilter.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(OpenTelemetryClientFilter.class));
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void createOpenTelemetry(
            OpenTelemetryConfig openTelemetryConfig,
            OpenTelemetryRecorder recorder,
            Optional<TracerProviderBuildItem> tracerProviderBuildItem,
            LaunchModeBuildItem launchMode) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT || launchMode.getLaunchMode() == LaunchMode.TEST) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        RuntimeValue<SdkTracerProvider> tracerProvider = tracerProviderBuildItem.map(TracerProviderBuildItem::getTracerProvider)
                .orElse(null);
        recorder.createOpenTelemetry(tracerProvider, openTelemetryConfig);
        recorder.eagerlyCreateContextStorage();
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void storeVertxOnContextStorage(OpenTelemetryRecorder recorder, CoreVertxBuildItem vertx) {
        recorder.storeVertxOnContextStorage(vertx.getVertx());
    }

    public static boolean isClassPresent(String classname) {
        return QuarkusClassLoader.isClassPresentAtRuntime(classname);
    }
}
