package io.quarkus.picocli.deployment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.picocli.runtime.DefaultPicocliCommandLineFactory;
import io.quarkus.picocli.runtime.PicocliRunner;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

class PicocliProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.PICOCLI);
    }

    @BuildStep
    void addScopeToCommands(BuildProducer<AutoAddScopeBuildItem> autoAddScope) {
        // First add @Dependent to all classes annotated with @Command that:
        // (a) require container services
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(DotName.createSimple(CommandLine.Command.class.getName()))
                .requiresContainerServices()
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(20)
                .unremovable()
                .build());
        // (b) or declare a single constructor with at least one parameter
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .match((clazz, annotations, index) -> {
                    List<MethodInfo> constructors = clazz.methods().stream().filter(m -> m.name().equals(MethodDescriptor.INIT))
                            .collect(Collectors.toList());
                    return constructors.size() == 1 && constructors.get(0).parametersCount() > 0;
                })
                .isAnnotatedWith(DotName.createSimple(CommandLine.Command.class.getName()))
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(10)
                .unremovable()
                .build());
        // Also add @Dependent to any class annotated with @TopCommand
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(DotName.createSimple(TopCommand.class.getName()))
                .defaultScope(BuiltinScope.DEPENDENT)
                .unremovable()
                .build());
    }

    @BuildStep
    IndexDependencyBuildItem picocliIndexDependency() {
        return new IndexDependencyBuildItem("info.picocli", "picocli");
    }

    @BuildStep
    void picocliRunner(ApplicationIndexBuildItem applicationIndex,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean,
            BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        IndexView index = combinedIndex.getIndex();
        Collection<DotName> topCommands = classesAnnotatedWith(index, TopCommand.class.getName());
        if (topCommands.isEmpty()) {
            List<DotName> commands = classesAnnotatedWith(applicationIndex.getIndex(),
                    CommandLine.Command.class.getName());
            if (commands.size() == 1) {
                // If there is exactly one @Command then make it @TopCommand
                DotName singleCommandClassName = commands.get(0);
                annotationsTransformer.produce(new AnnotationsTransformerBuildItem(
                        AnnotationsTransformer.appliedToClass()
                                .whenClass(c -> c.name().equals(singleCommandClassName))
                                // Make sure the transformation is applied before AutoAddScopeBuildItem is processed
                                .priority(2000)
                                .thenTransform(t -> t.add(TopCommand.class))));
            }
        }
        if (index.getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(PicocliRunner.class));
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DefaultPicocliCommandLineFactory.class));
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(PicocliRunner.class));
        }

        // Make all classes that can be instantiated by IFactory unremovable
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(CommandLine.ITypeConverter.class,
                CommandLine.IVersionProvider.class,
                CommandLine.IModelTransformer.class,
                CommandLine.IModelTransformer.class,
                CommandLine.IDefaultValueProvider.class,
                CommandLine.IParameterConsumer.class,
                CommandLine.IParameterPreprocessor.class,
                CommandLine.INegatableOptionTransformer.class,
                CommandLine.IHelpFactory.class));
    }

    private List<DotName> classesAnnotatedWith(IndexView indexView, String annotationClassName) {
        return indexView.getAnnotations(DotName.createSimple(annotationClassName))
                .stream().filter(ann -> ann.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(ann -> ann.target().asClass().name())
                .collect(Collectors.toList());
    }
}
