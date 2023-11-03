package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.contains;
import static io.quarkus.arc.processor.Annotations.containsAny;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Produces;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class AutoProducerMethodsProcessor {

    private static final Logger LOGGER = Logger.getLogger(AutoProducerMethodsProcessor.class);

    /**
     * Register an annotation transformer that automatically adds {@link Produces} to all non-void methods that are annotated
     * with a qualifier or a scope annotation.
     */
    @BuildStep
    void annotationTransformer(ArcConfig config, BeanArchiveIndexBuildItem beanArchiveIndex,
            CustomScopeAnnotationsBuildItem scopes,
            List<StereotypeRegistrarBuildItem> stereotypeRegistrars,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) throws Exception {
        if (!config.autoProducerMethods) {
            return;
        }
        Set<DotName> qualifiersAndStereotypes = new HashSet<>();
        for (AnnotationInstance qualifier : beanArchiveIndex.getIndex().getAnnotations(DotNames.QUALIFIER)) {
            qualifiersAndStereotypes.add(qualifier.target().asClass().name());
        }
        for (AnnotationInstance stereotype : beanArchiveIndex.getIndex().getAnnotations(DotNames.STEREOTYPE)) {
            qualifiersAndStereotypes.add(stereotype.target().asClass().name());
        }
        for (StereotypeRegistrarBuildItem stereotypeRegistrar : stereotypeRegistrars) {
            qualifiersAndStereotypes.addAll(stereotypeRegistrar.getStereotypeRegistrar().getAdditionalStereotypes());
        }
        LOGGER.debugf("Add missing @Produces to methods annotated with %s", qualifiersAndStereotypes);
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public int getPriority() {
                // Make sure this annotation transformer is invoked after the transformers with default priority
                return DEFAULT_PRIORITY - 1;
            }

            @Override
            public void transform(TransformationContext ctx) {
                if (ctx.getTarget().asMethod().returnType().kind() == org.jboss.jandex.Type.Kind.VOID) {
                    // Skip void methods
                    return;
                }
                Set<AnnotationInstance> methodAnnotations = Annotations.getAnnotations(Kind.METHOD, ctx.getAnnotations());
                if (methodAnnotations.isEmpty() || contains(methodAnnotations, DotNames.PRODUCES)
                        || contains(methodAnnotations, DotNames.INJECT)) {
                    // Skip methods with no annotations, initializers and methods already annotated with @Produces
                    return;
                }
                Set<AnnotationInstance> parameterAnnotations = Annotations.getAnnotations(Kind.METHOD_PARAMETER,
                        ctx.getAnnotations());
                if (contains(parameterAnnotations, DotNames.OBSERVES)
                        || contains(parameterAnnotations, DotNames.OBSERVES_ASYNC)
                        || contains(parameterAnnotations, DotNames.DISPOSES)) {
                    // Skip observers and disposers
                    return;
                }
                if (scopes.isScopeIn(methodAnnotations) || containsAny(methodAnnotations, qualifiersAndStereotypes)) {
                    ctx.transform().add(DotNames.PRODUCES).done();
                }
            }
        }));
    }

}
