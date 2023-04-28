package io.quarkus.arc.deployment.init;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
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
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.util.StringUtil;

/**
 * A processor that is used to track all {@link io.quarkus.deployment.builditem.InitTaskCompletedBuildItem} in order to exit
 * once they are completed if
 * needed.
 */
public class InitTaskProcessor {

    private static final Logger LOG = Logger.getLogger(InitTaskProcessor.class);

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
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        IndexView index = indexBuildItem.getIndex();
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        Set<String> generatedBeanNames = new HashSet<>();

        for (AnnotationInstance instance : index.getAnnotations(INITIALIZATION_ANNOTATION)) {
            AnnotationTarget annotationTarget = instance.target();
            if (annotationTarget instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) annotationTarget;
                String fqcn = classInfo.name().toString();
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));

                String taskName = StringUtil.hyphenate(instance.value() != null
                        ? instance.value().asString()
                        : classInfo.name().withoutPackagePrefix());

                registerInitTask(taskName, initTasks);
            }

            if (annotationTarget instanceof MethodInfo) {
                MethodInfo methodInfo = (MethodInfo) annotationTarget;
                String fqcn = methodInfo.declaringClass().name().toString();
                String runnableFqcn = fqcn + "_GeneratedRunnable";
                String methodSignature = methodInfo.name() + methodInfo.parameters()
                        .stream()
                        .map(t -> t.name().toString())
                        .collect(Collectors.joining(",", "(", ")"));

                if (methodInfo.parameters().size() != 0) {
                    LOG.warn("Using @Initialization on methods with arguments is not supported. Ignoring method "
                            + methodInfo.declaringClass().name().toString() + "." + methodSignature);
                    continue;
                }

                if (generatedBeanNames.contains(runnableFqcn)) {
                    LOG.warn("A class can only have a single method annotated with @Initialization. Ignoring: "
                            + methodInfo.declaringClass().name().toString() + "." + methodSignature);
                    continue;
                }

                MethodDescriptor annotatedMethod = MethodDescriptor.of(methodInfo);
                ClassCreator runnableClass = ClassCreator.builder().classOutput(classOutput)
                        .className(runnableFqcn)
                        .interfaces(Runnable.class)
                        .build();

                runnableClass.addAnnotation(AnnotationInstance.create(INITIALIZATION_ANNOTATION, annotationTarget,
                        Optional.ofNullable(instance.value()).map(List::of).orElse(List.of())));

                runnableClass.addAnnotation(ApplicationScoped.class);
                runnableClass.addAnnotation(Unremovable.class);

                FieldCreator delegate = runnableClass.getFieldCreator("task", fqcn).setModifiers(ACC_PUBLIC);
                delegate.addAnnotation(Inject.class);

                MethodCreator runMethod = runnableClass.getMethodCreator("run", void.class);
                runMethod.addAnnotation(Override.class.getCanonicalName(), RetentionPolicy.RUNTIME);
                runMethod.invokeVirtualMethod(annotatedMethod,
                        runMethod.readInstanceField(delegate.getFieldDescriptor(), runMethod.getThis()));
                runMethod.returnValue(null);
                runnableClass.close();

                generatedBeanNames.add(runnableFqcn);
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));

                String taskName = StringUtil.hyphenate(instance.value() != null
                        ? instance.value().asString()
                        : methodInfo.declaringClass().name().withoutPackagePrefix());
                registerInitTask(taskName, initTasks);
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

    void registerInitTask(String taskName, BuildProducer<InitTaskBuildItem> initTasks) {
                        initTasks.produce(InitTaskBuildItem.create()
                        .withName(taskName)
                        .withTaskEnvVars(Map.of("QUARKUS_INIT_AND_EXIT", "true", "QUARKUS_INIT_TASK_FILTER", taskName))
                        .withAppEnvVars(Map.of("QUARKUS_INIT_DISABLED", "true"))
                        .withSharedEnvironment(true)
                        .withSharedFilesystem(true));

    }
}
