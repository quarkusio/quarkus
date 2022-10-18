package io.quarkus.opentelemetry.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.deployment.tracing.TracerProviderBuildItem;
import io.quarkus.opentelemetry.runtime.OpenTelemetryProducer;
import io.quarkus.opentelemetry.runtime.OpenTelemetryRecorder;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.opentelemetry.runtime.config.OpenTelemetryConfig;
import io.quarkus.opentelemetry.runtime.tracing.cdi.WithSpanInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.InstrumentationRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryProcessor {

    private static final DotName LEGACY_WITH_SPAN = DotName.createSimple(
            io.opentelemetry.extension.annotations.WithSpan.class.getName());
    private static final DotName WITH_SPAN = DotName.createSimple(WithSpan.class.getName());
    private static final DotName SPAN_KIND = DotName.createSimple(SpanKind.class.getName());
    private static final DotName WITH_SPAN_INTERCEPTOR = DotName.createSimple(WithSpanInterceptor.class.getName());
    private static final DotName LEGACY_SPAN_ATRIBUTE = DotName.createSimple(
            io.opentelemetry.extension.annotations.SpanAttribute.class.getName());;
    private static final DotName SPAN_ATTRIBUTE = DotName.createSimple(SpanAttribute.class.getName());

    @BuildStep
    AdditionalBeanBuildItem ensureProducerIsRetained() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(OpenTelemetryProducer.class)
                .build();
    }

    @BuildStep
    void registerOpenTelemetryContextStorage(
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/io.opentelemetry.context.ContextStorageProvider"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, QuarkusContextStorage.class));
    }

    @BuildStep
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

    @BuildStep
    void transformWithSpan(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        // Transform deprecated annotation into new one
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public void transform(TransformationContext context) {
                final AnnotationTarget target = context.getTarget();

                List<AnnotationInstance> legacyWithSpans = context.getAnnotations().stream()
                        .filter(annotationInstance -> annotationInstance.name().equals(LEGACY_WITH_SPAN))
                        .collect(Collectors.toList());

                for (AnnotationInstance legacyAnnotation : legacyWithSpans) {
                    AnnotationValue value = Optional.ofNullable(legacyAnnotation.value("value"))
                            .orElse(AnnotationValue.createStringValue("value", ""));
                    AnnotationValue kind = Optional.ofNullable(legacyAnnotation.value("kind"))
                            .orElse(AnnotationValue.createEnumValue("kind", SPAN_KIND, SpanKind.INTERNAL.name()));
                    AnnotationInstance annotation = AnnotationInstance.create(
                            WITH_SPAN,
                            target,
                            List.of(value, kind));
                    context.transform().add(annotation).done();
                }
            }
        }));

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(transformationContext -> {
            AnnotationTarget target = transformationContext.getTarget();
            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                if (target.asClass().name().equals(WITH_SPAN_INTERCEPTOR)) {
                    transformationContext.transform().add(WITH_SPAN).done();
                }
            }
        }));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createOpenTelemetry(
            OpenTelemetryConfig openTelemetryConfig,
            OpenTelemetryRecorder recorder,
            InstrumentationRecorder instrumentationRecorder,
            Optional<TracerProviderBuildItem> tracerProviderBuildItem,
            LaunchModeBuildItem launchMode) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT || launchMode.getLaunchMode() == LaunchMode.TEST) {
            recorder.resetGlobalOpenTelemetryForDevMode();
        }

        RuntimeValue<SdkTracerProvider> tracerProvider = tracerProviderBuildItem.map(TracerProviderBuildItem::getTracerProvider)
                .orElse(null);
        recorder.createOpenTelemetry(tracerProvider, openTelemetryConfig);
        recorder.eagerlyCreateContextStorage();

        // just checking for live reload would bypass the OpenTelemetryDevModeTest
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            instrumentationRecorder.setTracerDevMode(instrumentationRecorder.createTracers());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void storeVertxOnContextStorage(OpenTelemetryRecorder recorder, CoreVertxBuildItem vertx) {
        recorder.storeVertxOnContextStorage(vertx.getVertx());
    }
}
