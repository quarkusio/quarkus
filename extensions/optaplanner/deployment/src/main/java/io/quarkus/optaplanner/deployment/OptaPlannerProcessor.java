package io.quarkus.optaplanner.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.drools.core.base.ClassFieldAccessorFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreCalculator;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.optaplanner.OptaPlannerBeanProvider;
import io.quarkus.optaplanner.OptaPlannerRecorder;
import io.quarkus.runtime.configuration.ConfigurationException;

class OptaPlannerProcessor {

    OptaPlannerBuildTimeConfig optaPlannerBuildTimeConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.OPTAPLANNER);
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchSolverConfigXml() {
        String solverConfigXML;
        if (optaPlannerBuildTimeConfig.solverConfigXml.isPresent()) {
            solverConfigXML = optaPlannerBuildTimeConfig.solverConfigXml.get();
        } else {
            solverConfigXML = OptaPlannerBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL;
        }
        return new HotDeploymentWatchedFileBuildItem(solverConfigXML);
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchScoreDrl() {
        return new HotDeploymentWatchedFileBuildItem(SolverBuildTimeConfig.DEFAULT_SCORE_DRL_URL);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // The bean encapsulating the SolverFactory
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerBeanProvider.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void recordSolverFactory(OptaPlannerRecorder recorder, RecorderContext recorderContext,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SolverConfig solverConfig;
        if (optaPlannerBuildTimeConfig.solverConfigXml.isPresent()) {
            String solverConfigXML = optaPlannerBuildTimeConfig.solverConfigXml.get();
            if (classLoader.getResource(solverConfigXML) == null) {
                throw new ConfigurationException("Invalid quarkus.optaplanner.solverConfigXML property ("
                        + solverConfigXML + "): that classpath resource does not exist.");
            }
            solverConfig = SolverConfig.createFromXmlResource(solverConfigXML, classLoader);
        } else if (classLoader.getResource(OptaPlannerBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL) != null) {
            solverConfig = SolverConfig.createFromXmlResource(
                    OptaPlannerBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL, classLoader);
        } else {
            solverConfig = new SolverConfig(classLoader);
        }
        // The deployment classLoader must not escape to runtime
        solverConfig.setClassLoader(null);

        IndexView indexView = combinedIndex.getIndex();
        applySolverProperties(recorderContext, indexView, solverConfig);

        if (solverConfig.getSolutionClass() != null) {
            Type jandexType = Type.create(DotName.createSimple(solverConfig.getSolutionClass().getName()), Type.Kind.CLASS);
            reflectiveHierarchyClass.produce(new ReflectiveHierarchyBuildItem(jandexType));
        }
        List<Class<?>> reflectiveClassList = new ArrayList<>(5);
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = solverConfig.getScoreDirectorFactoryConfig();
        if (scoreDirectorFactoryConfig != null) {
            if (scoreDirectorFactoryConfig.getEasyScoreCalculatorClass() != null) {
                reflectiveClassList.add(scoreDirectorFactoryConfig.getEasyScoreCalculatorClass());
            }
            if (scoreDirectorFactoryConfig.getConstraintProviderClass() != null) {
                reflectiveClassList.add(scoreDirectorFactoryConfig.getConstraintProviderClass());
            }
            if (scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass() != null) {
                reflectiveClassList.add(scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass());
            }
        }
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(true, false, false,
                        reflectiveClassList.stream().map(Class::getName).toArray(String[]::new)));

        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        optaPlannerBuildTimeConfig.solverManager.parallelSolverCount.ifPresent(solverManagerConfig::setParallelSolverCount);
        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initialize(solverConfig, solverManagerConfig)));
    }

    private void applySolverProperties(RecorderContext recorderContext,
            IndexView indexView, SolverConfig solverConfig) {
        if (solverConfig.getScanAnnotatedClassesConfig() != null) {
            throw new IllegalArgumentException("Do not use scanAnnotatedClasses with the Quarkus extension,"
                    + " because the Quarkus extension scans too.\n"
                    + "Maybe delete the scanAnnotatedClasses element in the solver config.");
        }
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(findSolutionClass(recorderContext, indexView));
        }
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(findEntityClassList(recorderContext, indexView));
        }
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
            scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(
                    findImplementingClass(EasyScoreCalculator.class, indexView));
            scoreDirectorFactoryConfig.setConstraintProviderClass(
                    findImplementingClass(ConstraintProvider.class, indexView));
            scoreDirectorFactoryConfig.setIncrementalScoreCalculatorClass(
                    findImplementingClass(IncrementalScoreCalculator.class, indexView));
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader.getResource(SolverBuildTimeConfig.DEFAULT_SCORE_DRL_URL) != null) {
                scoreDirectorFactoryConfig.setScoreDrlList(Collections.singletonList(
                        SolverBuildTimeConfig.DEFAULT_SCORE_DRL_URL));
            }
            solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        }
        optaPlannerBuildTimeConfig.solver.environmentMode.ifPresent(solverConfig::setEnvironmentMode);
        optaPlannerBuildTimeConfig.solver.moveThreadCount.ifPresent(solverConfig::setMoveThreadCount);
        applyTerminationProperties(solverConfig);
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

    private <T> Class<? extends T> findImplementingClass(Class<T> targetClass, IndexView indexView) {
        Collection<ClassInfo> classInfos = indexView.getAllKnownImplementors(
                DotName.createSimple(targetClass.getName()));
        if (classInfos.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + convertClassInfosToString(classInfos)
                    + ") found that implement the interface " + targetClass.getSimpleName() + ".");
        }
        if (classInfos.isEmpty()) {
            return null;
        }
        String className = classInfos.iterator().next().name().toString();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Don't use recorderContext.classProxy(className)
            // because ReflectiveClassBuildItem cannot cope with a class proxy
            return (Class<? extends T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + className
                    + ") cannot be created during deployment.", e);
        }
    }

    private void applyTerminationProperties(SolverConfig solverConfig) {
        TerminationConfig terminationConfig = solverConfig.getTerminationConfig();
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
            solverConfig.setTerminationConfig(terminationConfig);
        }
        optaPlannerBuildTimeConfig.solver.termination.spentLimit.ifPresent(terminationConfig::setSpentLimit);
        optaPlannerBuildTimeConfig.solver.termination.unimprovedSpentLimit
                .ifPresent(terminationConfig::setUnimprovedSpentLimit);
        optaPlannerBuildTimeConfig.solver.termination.bestScoreLimit.ifPresent(terminationConfig::setBestScoreLimit);
    }

    private String convertAnnotationInstancesToString(Collection<AnnotationInstance> annotationInstances) {
        return "[" + annotationInstances.stream().map(instance -> instance.target().toString())
                .collect(Collectors.joining(", ")) + "]";
    }

    private String convertClassInfosToString(Collection<ClassInfo> classInfos) {
        return "[" + classInfos.stream().map(instance -> instance.name().toString())
                .collect(Collectors.joining(", ")) + "]";
    }

    @BuildStep
    public RuntimeInitializedClassBuildItem nativeImageDroolsTricks() {
        return new RuntimeInitializedClassBuildItem(ClassFieldAccessorFactory.class.getName());
    }

}
