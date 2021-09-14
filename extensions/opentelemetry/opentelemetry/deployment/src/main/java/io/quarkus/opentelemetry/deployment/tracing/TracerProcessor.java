package io.quarkus.opentelemetry.deployment.tracing;

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.opentelemetry.runtime.OpenTelemetryConfig;
import io.quarkus.opentelemetry.runtime.tracing.TracerProducer;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.core.deployment.VertxOptionsConsumerBuildItem;

public class TracerProcessor {
    private static final DotName ID_GENERATOR = DotName.createSimple(IdGenerator.class.getName());
    private static final DotName RESOURCE = DotName.createSimple(Resource.class.getName());
    private static final DotName SAMPLER = DotName.createSimple(Sampler.class.getName());
    private static final DotName SPAN_EXPORTER = DotName.createSimple(SpanExporter.class.getName());
    private static final DotName SPAN_PROCESSOR = DotName.createSimple(SpanProcessor.class.getName());

    public static class TracerEnabled implements BooleanSupplier {
        OpenTelemetryConfig otelConfig;

        public boolean getAsBoolean() {
            return otelConfig.tracer.enabled.map(tracerEnabled -> otelConfig.enabled && tracerEnabled)
                    .orElseGet(() -> otelConfig.enabled);
        }
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    UnremovableBeanBuildItem ensureProducersAreRetained(
            CombinedIndexBuildItem indexBuildItem,
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isPresent(Capability.OPENTRACING) ||
                capabilities.isPresent(Capability.SMALLRYE_OPENTRACING)) {
            throw new ConfigurationException("The OpenTelemetry extension tracer can not be used in " +
                    "conjunction with either the SmallRye OpenTracing or Jaeger extensions.");
        }

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(TracerProducer.class)
                .build());

        IndexView index = indexBuildItem.getIndex();

        // Find all known SpanExporters and SpanProcessors
        Collection<String> knownClasses = new HashSet<>();
        knownClasses.add(ID_GENERATOR.toString());
        index.getAllKnownImplementors(ID_GENERATOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(RESOURCE.toString());
        index.getAllKnownImplementors(RESOURCE)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SAMPLER.toString());
        index.getAllKnownImplementors(SAMPLER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SPAN_EXPORTER.toString());
        index.getAllKnownImplementors(SPAN_EXPORTER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));
        knownClasses.add(SPAN_PROCESSOR.toString());
        index.getAllKnownImplementors(SPAN_PROCESSOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        Set<String> retainProducers = new HashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(DotNames.PRODUCES)) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD:
                    MethodInfo method = target.asMethod();
                    String returnType = method.returnType().name().toString();
                    if (knownClasses.contains(returnType)) {
                        retainProducers.add(method.declaringClass().name().toString());
                    }
                    break;
                case FIELD:
                    FieldInfo field = target.asField();
                    String fieldType = field.type().name().toString();
                    if (knownClasses.contains(fieldType)) {
                        retainProducers.add(field.declaringClass().name().toString());
                    }
                    break;
                default:
                    break;
            }
        }

        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(retainProducers));
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem vertxTracingOptions(TracerRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.setVertxTracingOptions(), LIBRARY_AFTER);
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    TracerProviderBuildItem createTracerProvider(
            TracerRecorder recorder,
            ApplicationInfoBuildItem appInfo,
            ShutdownContextBuildItem shutdownContext,
            BeanContainerBuildItem beanContainerBuildItem) {
        String serviceName = appInfo.getName();
        String serviceVersion = appInfo.getVersion();
        return new TracerProviderBuildItem(
                recorder.createTracerProvider(Version.getVersion(), serviceName, serviceVersion, shutdownContext));
    }

    @BuildStep(onlyIf = TracerEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTracer(TracerRuntimeConfig runtimeConfig,
            TracerRecorder recorder) {
        recorder.setupResources(runtimeConfig);
        recorder.setupSampler(runtimeConfig);
        recorder.setupVertxTracer();
    }
}
