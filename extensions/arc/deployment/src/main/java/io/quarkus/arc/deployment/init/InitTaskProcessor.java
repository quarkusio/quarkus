package io.quarkus.arc.deployment.init;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.init.InitTaskRecorder;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.runtime.util.StringUtil;

/**
 * A processor that is used to track all {@link io.quarkus.deployment.builditem.InitTaskCompletedBuildItem} in order to exit
 * once they are completed if
 * needed.
 */
public class InitTaskProcessor {

    private static final DotName INITIALIZATION_ANNOTATION = DotName
            .createSimple("io.quarkus.runtime.annotations.Initialization");

    @BuildStep
    QualifierRegistrarBuildItem registerInitialization() {
        return new QualifierRegistrarBuildItem(new QualifierRegistrar() {
            @Override
            public Map<DotName, Set<String>> getAdditionalQualifiers() {
                return Map.of(INITIALIZATION_ANNOTATION, Collections.emptySet());
            }
        });
    }

    @BuildStep
    void process(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<InitTaskBuildItem> initTasks,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        IndexView index = indexBuildItem.getIndex();
        for (AnnotationInstance instance : index.getAnnotations(INITIALIZATION_ANNOTATION)) {
            AnnotationTarget annotationTarget = instance.target();
            if (annotationTarget instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) annotationTarget;
                String className = classInfo.name().toString();
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(className));

                String taskName = StringUtil.hyphenate(instance.value() != null
                        ? instance.value().asString()
                        : classInfo.name().withoutPackagePrefix());

                initTasks.produce(InitTaskBuildItem.create()
                        .withName(taskName)
                        .withTaskEnvVars(Map.of("QUARKUS_INIT_AND_EXIT", "true"))
                        .withAppEnvVars(Map.of("QUARKUS_INIT_DISABLED", "true"))
                        .withSharedEnvironment(true)
                        .withSharedFilesystem(true));
            }
        }
    }

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void executeUserInitTasks(InitTaskRecorder recorder,
            List<InitTaskBuildItem> initTasks,
            BuildProducer<InitTaskCompletedBuildItem> initializationCompleteBuildItem) {
        for (InitTaskBuildItem initTask : initTasks) {
            String taskName = initTask.getName();
            recorder.executeInitializationTask(taskName);
            initializationCompleteBuildItem.produce(new InitTaskCompletedBuildItem(taskName));
        }
    }

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void startApplicationInitializer(InitTaskRecorder recorder, List<InitTaskCompletedBuildItem> initTaskCompletedBuildItems) {
        recorder.exitIfNeeded();
    }
}
