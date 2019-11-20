package io.quarkus.optaplanner.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.optaplanner.OptaPlannerRecorder;
import io.quarkus.optaplanner.SolverFactoryProvider;

class OptaPlannerProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.OPTAPLANNER);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // The bean encapsulating the SolverFactory
        additionalBeans.produce(new AdditionalBeanBuildItem(SolverFactoryProvider.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void recordSolverFactory(OptaPlannerRecorder recorder, RecorderContext recorderContext,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) {
        IndexView indexView = combinedIndex.getIndex();
        Class<?> solutionClass = findSolutionClass(recorderContext, indexView);
        List<Class<?>> entityClassList = findEntityClassList(recorderContext, indexView);
        Class<? extends ConstraintProvider> constraintProviderClass = findConstraintProviderClass(recorderContext, indexView);
        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initializeSolverFactory(solutionClass, entityClassList, constraintProviderClass)));
    }

    private Class<?> findSolutionClass(RecorderContext recorderContext, IndexView indexView) {
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
        if (annotationInstances.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        if (annotationInstances.isEmpty()) {
            throw new IllegalStateException("No classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        AnnotationTarget solutionTarget = annotationInstances.iterator().next().target();
        if (solutionTarget.kind() != AnnotationTarget.Kind.CLASS) {
            throw new IllegalStateException("A target (" + solutionTarget
                    + ") with a @" + PlanningSolution.class.getSimpleName() + " must be a class.");
        }

        return recorderContext.classProxy(solutionTarget.asClass().name().toString());
    }

    private List<Class<?>> findEntityClassList(RecorderContext recorderContext, IndexView indexView) {
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_ENTITY);
        if (annotationInstances.isEmpty()) {
            throw new IllegalStateException("No classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningEntity.class.getSimpleName() + " annotation.");
        }
        List<AnnotationTarget> targetList = annotationInstances.stream()
                .map(AnnotationInstance::target)
                .collect(Collectors.toList());
        if (targetList.stream().anyMatch(target -> target.kind() != AnnotationTarget.Kind.CLASS)) {
            throw new IllegalStateException("All targets (" + targetList
                    + ") with a @" + PlanningEntity.class.getSimpleName() + " must be a class.");
        }
        return targetList.stream()
                .map(target -> recorderContext.classProxy(target.asClass().name().toString()))
                .collect(Collectors.toList());
    }

    private Class<? extends ConstraintProvider> findConstraintProviderClass(RecorderContext recorderContext,
            IndexView indexView) {
        Collection<ClassInfo> classInfos = indexView.getAllKnownImplementors(
                DotName.createSimple(ConstraintProvider.class.getName()));
        if (classInfos.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + convertClassInfosToString(classInfos)
                    + ") found that implement the interface " + ConstraintProvider.class.getSimpleName() + ".");
        }
        if (classInfos.isEmpty()) {
            throw new IllegalStateException("No classes (" + convertClassInfosToString(classInfos)
                    + ") found that implement the interface " + ConstraintProvider.class.getSimpleName() + ".");
        }
        // TODO use .asSubclass(ConstraintProvider.class) once https://github.com/quarkusio/quarkus/issues/5630 is fixed
        return (Class<? extends ConstraintProvider>) recorderContext.classProxy(classInfos.iterator().next().name().toString());
    }

    private String convertAnnotationInstancesToString(Collection<AnnotationInstance> annotationInstances) {
        return "[" + annotationInstances.stream().map(instance -> instance.target().toString())
                .collect(Collectors.joining(", ")) + "]";
    }

    private String convertClassInfosToString(Collection<ClassInfo> classInfos) {
        return "[" + classInfos.stream().map(instance -> instance.name().toString())
                .collect(Collectors.joining(", ")) + "]";
    }

    // TODO health check

    // TODO JSON & JPA customization for Score.class

}
