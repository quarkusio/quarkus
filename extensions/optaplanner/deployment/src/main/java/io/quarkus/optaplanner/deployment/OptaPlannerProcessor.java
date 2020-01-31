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
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;
import io.quarkus.optaplanner.OptaPlannerBeanProvider;
import io.quarkus.optaplanner.OptaPlannerRecorder;

class OptaPlannerProcessor {

    private static final String OPTAPLANNER_JACKSON_MODULE = "org.optaplanner.persistence.jackson.api.OptaPlannerJacksonModule";

    OptaPlannerQuarkusConfig optaPlannerQuarkusConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.OPTAPLANNER);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        // The bean encapsulating the SolverFactory
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerBeanProvider.class));
    }

    @BuildStep(loadsApplicationClasses = true)
    @Record(STATIC_INIT)
    void recordSolverFactory(OptaPlannerRecorder recorder, RecorderContext recorderContext,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SolverConfig solverConfig;
        if (optaPlannerQuarkusConfig.solverConfigXml.isPresent()) {
            String solverConfigXML = optaPlannerQuarkusConfig.solverConfigXml.get();
            if (classLoader.getResource(solverConfigXML) == null) {
                throw new IllegalStateException("Invalid optaplanner.solverConfigXML property (" + solverConfigXML
                        + "): that classpath resource does not exist.");
            }
            solverConfig = SolverConfig.createFromXmlResource(solverConfigXML, classLoader);
        } else if (classLoader.getResource(OptaPlannerQuarkusConfig.DEFAULT_SOLVER_CONFIG_URL) != null) {
            solverConfig = SolverConfig.createFromXmlResource(
                    OptaPlannerQuarkusConfig.DEFAULT_SOLVER_CONFIG_URL, classLoader);
        } else {
            solverConfig = new SolverConfig(classLoader);
        }
        solverConfig.setClassLoader(null); // TODO HACK

        IndexView indexView = combinedIndex.getIndex();
        applySolverProperties(recorder, recorderContext, indexView, solverConfig);

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initialize(solverConfig)));
    }

    private void applySolverProperties(OptaPlannerRecorder recorder, RecorderContext recorderContext,
            IndexView indexView, SolverConfig solverConfig) {
        //        if (solverProperties.getEnvironmentMode() != null) {
        //            solverConfig.setEnvironmentMode(solverProperties.getEnvironmentMode());
        //        }
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(findSolutionClass(recorderContext, indexView));
        }
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(findEntityClassList(recorderContext, indexView));
        }
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
            // Use Bavet to avoid Drools classpath issues (drools 7 vs kogito 1 code duplication)
            scoreDirectorFactoryConfig.setConstraintStreamImplType(ConstraintStreamImplType.BAVET);
            scoreDirectorFactoryConfig.setConstraintProviderClass(findConstraintProviderClass(recorderContext, indexView));
            solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        }
        //        applyTerminationProperties(solverConfig);
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

    @BuildStep
    void registerOptaPlannerJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        try {
            Class.forName(OPTAPLANNER_JACKSON_MODULE, false, Thread.currentThread().getContextClassLoader());
        } catch (Exception ignored) {
            return;
        }
        classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(OPTAPLANNER_JACKSON_MODULE));
    }

    // TODO JPA customization for Score.class

}
