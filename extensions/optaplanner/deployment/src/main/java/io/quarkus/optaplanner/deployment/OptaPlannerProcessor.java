package io.quarkus.optaplanner.deployment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.optaplanner.OptaPlannerRecorder;
import io.quarkus.optaplanner.SolverFactoryProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

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
    void recordSolverFactory(OptaPlannerRecorder recorder,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) {
        IndexView indexView = combinedIndex.getIndex();
        String solutionClassName = findSolutionClassName(indexView);
        List<String> entityClassNameList = findEntityClassNameList(indexView);
        String constraintProviderClassName = findConstraintProviderClassName(indexView);
        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initializeSolverFactory(solutionClassName, entityClassNameList, constraintProviderClassName)));
    }

    private String findSolutionClassName(IndexView indexView) {
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

        return solutionTarget.asClass().name().toString();
    }

    private List<String> findEntityClassNameList(IndexView indexView) {
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
        return targetList.stream().map(target -> target.asClass().name().toString()).collect(Collectors.toList());
    }

    private String findConstraintProviderClassName(IndexView indexView) {
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
        return classInfos.iterator().next().name().toString();
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
