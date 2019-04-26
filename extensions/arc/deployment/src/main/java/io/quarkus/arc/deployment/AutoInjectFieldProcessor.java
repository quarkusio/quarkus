package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class AutoInjectFieldProcessor {

    private static final Logger LOGGER = Logger.getLogger(AutoInjectFieldProcessor.class);

    @BuildStep
    void autoInjectQualifiers(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<AutoInjectAnnotationBuildItem> autoInjectAnnotations) {
        List<DotName> qualifiers = new ArrayList<>();
        for (AnnotationInstance qualifier : beanArchiveIndex.getIndex().getAnnotations(DotNames.QUALIFIER)) {
            qualifiers.add(qualifier.target().asClass().name());
        }
        autoInjectAnnotations.produce(new AutoInjectAnnotationBuildItem(qualifiers));
    }

    /**
     * Uses {@link AnnotationsTransformer} to automatically add {@code @Inject} to all non-static fields that are annotated with
     * one of the specified annotations.
     */
    @BuildStep
    void annotationTransformer(ArcConfig config, List<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) throws Exception {
        if (!config.autoInjectFields) {
            return;
        }
        Set<DotName> annotations = new HashSet<>();
        for (AutoInjectAnnotationBuildItem autoInjectAnnotation : autoInjectAnnotations) {
            annotations.addAll(autoInjectAnnotation.getAnnotationNames());
        }
        if (annotations.isEmpty()) {
            return;
        }
        LOGGER.debugf("Add missing @Inject to fields annotated with %s", annotations);
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.FIELD;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                FieldInfo field = transformationContext.getTarget().asField();
                if (Modifier.isStatic(field.flags()) || field.hasAnnotation(DotNames.INJECT)) {
                    return;
                }
                for (DotName annotation : annotations) {
                    if (field.hasAnnotation(annotation)) {
                        transformationContext.transform().add(DotNames.INJECT).done();
                        return;
                    }
                }
            }
        }));
    }

}
