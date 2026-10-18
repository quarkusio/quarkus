package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.contains;
import static io.quarkus.arc.processor.Annotations.containsAny;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            BuildProducer<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            List<QualifierRegistrarBuildItem> qualifierRegistrars) {
        List<DotName> qualifiers = new ArrayList<>();
        for (AnnotationInstance qualifier : beanArchiveIndex.getIndex().getAnnotations(DotNames.QUALIFIER)) {
            qualifiers.add(qualifier.target().asClass().name());
        }
        for (QualifierRegistrarBuildItem registrar : qualifierRegistrars) {
            qualifiers.addAll(registrar.getQualifierRegistrar().getAdditionalQualifiers().keySet());
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
        if (!config.autoInjectFields()) {
            return;
        }
        List<DotName> annotationNames = new ArrayList<>();
        for (AutoInjectAnnotationBuildItem autoInjectAnnotation : autoInjectAnnotations) {
            annotationNames.addAll(autoInjectAnnotation.getAnnotationNames());
        }
        if (annotationNames.isEmpty()) {
            return;
        }
        LOGGER.debugf("Add missing @Inject to fields annotated with %s", annotationNames);
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.FIELD;
            }

            @Override
            public int getPriority() {
                // Make sure this annotation transformer is invoked after the transformers with default priority
                return DEFAULT_PRIORITY - 1;
            }

            @Override
            public void transform(TransformationContext ctx) {
                Collection<AnnotationInstance> fieldAnnotations = ctx.getAnnotations();
                FieldInfo field = ctx.getTarget().asField();
                if (Modifier.isStatic(field.flags())
                        || Modifier.isFinal(field.flags())
                        || contains(fieldAnnotations, DotNames.INJECT)
                        || contains(fieldAnnotations, DotNames.PRODUCES)) {
                    return;
                }
                if (containsAny(fieldAnnotations, annotationNames)) {
                    ctx.transform().add(DotNames.INJECT).done();
                    return;
                }
            }
        }));
    }

}
