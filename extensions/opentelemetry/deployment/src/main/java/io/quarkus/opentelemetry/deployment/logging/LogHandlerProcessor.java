package io.quarkus.opentelemetry.deployment.logging;

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

import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.agroal.spi.OpenTelemetryInitBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogRecorder;

@BuildSteps(onlyIf = LogHandlerProcessor.LogsEnabled.class)
class LogHandlerProcessor {

    private static final DotName LOG_RECORD_EXPORTER = DotName.createSimple(LogRecordExporter.class.getName());
    private static final DotName LOG_RECORD_PROCESSOR = DotName.createSimple(LogRecordProcessor.class.getName());
    //    private static final DotName LOG_RECORD_HANDLER = DotName.createSimple(Handler.class.getName());

    @BuildStep
    UnremovableBeanBuildItem ensureProducersAreRetained(
            CombinedIndexBuildItem indexBuildItem) {

        IndexView index = indexBuildItem.getIndex();

        // Find all known SpanExporters and SpanProcessors
        Collection<String> knownClasses = new HashSet<>();
        knownClasses.add(LOG_RECORD_EXPORTER.toString());
        index.getAllKnownImplementors(LOG_RECORD_EXPORTER)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        knownClasses.add(LOG_RECORD_PROCESSOR.toString());
        index.getAllKnownImplementors(LOG_RECORD_PROCESSOR)
                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

        //        knownClasses.add(LOG_RECORD_HANDLER.toString());
        //        index.getAllKnownImplementors(LOG_RECORD_HANDLER)
        //                .forEach(classInfo -> knownClasses.add(classInfo.name().toString()));

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

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(OpenTelemetryInitBuildItem.class)
    LogHandlerBuildItem build(OpenTelemetryLogRecorder recorder,
            OpenTelemetryLogConfig config,
            BeanContainerBuildItem beanContainerBuildItem) {
        return new LogHandlerBuildItem(recorder.initializeHandler(beanContainerBuildItem.getValue(), config));
    }

    public static class LogsEnabled implements BooleanSupplier {
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.logs().enabled()
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
