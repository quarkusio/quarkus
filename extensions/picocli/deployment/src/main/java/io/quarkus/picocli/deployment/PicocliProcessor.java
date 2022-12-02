package io.quarkus.picocli.deployment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
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
    BeanDefiningAnnotationBuildItem commandBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(DotName.createSimple(CommandLine.Command.class.getName()));
    }

    @BuildStep
    IndexDependencyBuildItem picocliIndexDependency() {
        return new IndexDependencyBuildItem("info.picocli", "picocli");
    }

    @BuildStep
    void picocliRunner(ApplicationIndexBuildItem applicationIndex,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        IndexView index = combinedIndex.getIndex();
        Collection<DotName> topCommands = classesAnnotatedWith(index, TopCommand.class.getName());
        if (topCommands.isEmpty()) {
            List<DotName> commands = classesAnnotatedWith(applicationIndex.getIndex(),
                    CommandLine.Command.class.getName());
            if (commands.size() == 1) {
                annotationsTransformer.produce(createAnnotationTransformer(commands.get(0)));
            }
        }
        if (index.getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(PicocliRunner.class));
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DefaultPicocliCommandLineFactory.class));
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(PicocliRunner.class));
        }
    }

    private AnnotationsTransformerBuildItem createAnnotationTransformer(DotName className) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                ClassInfo target = context.getTarget().asClass();
                if (target.name().equals(className)) {
                    context.transform().add(TopCommand.class).done();
                }
            }
        });
    }

    private List<DotName> classesAnnotatedWith(IndexView indexView, String annotationClassName) {
        return indexView.getAnnotations(DotName.createSimple(annotationClassName))
                .stream().filter(ann -> ann.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(ann -> ann.target().asClass().name())
                .collect(Collectors.toList());
    }
}
