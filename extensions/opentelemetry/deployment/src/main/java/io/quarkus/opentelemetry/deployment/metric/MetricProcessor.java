package io.quarkus.opentelemetry.deployment.metric;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.metrics.cdi.MetricsProducer;

@BuildSteps(onlyIf = MetricProcessor.MetricEnabled.class)
public class MetricProcessor {
    private static final DotName METRIC_EXPORTER = DotName.createSimple(MetricExporter.class.getName());
    private static final DotName METRIC_READER = DotName.createSimple(MetricReader.class.getName());
    private static final DotName METRIC_PROCESSOR = DotName.createSimple(MetricProcessor.class.getName());

    @BuildStep
    UnremovableBeanBuildItem ensureProducersAreRetained(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(MetricsProducer.class)
                .build());

        IndexView index = indexBuildItem.getIndex();

        // Find all known SpanExporters and SpanProcessors
        Collection<String> knownClasses = new HashSet<>();
        knownClasses.add(METRIC_EXPORTER.toString());
        index.getAllKnownImplementors(METRIC_EXPORTER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        knownClasses.add(METRIC_READER.toString());
        index.getAllKnownImplementors(METRIC_READER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        knownClasses.add(METRIC_PROCESSOR.toString());
        index.getAllKnownImplementors(METRIC_PROCESSOR)
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

    public static class MetricEnabled implements BooleanSupplier {
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.metrics().enabled()
                    .map(new Function<Boolean, Boolean>() {
                        @Override
                        public Boolean apply(Boolean enabled) {
                            return otelBuildConfig.enabled() && enabled;
                        }
                    })
                    .orElseGet(() -> otelBuildConfig.enabled());
        }
    }
}
