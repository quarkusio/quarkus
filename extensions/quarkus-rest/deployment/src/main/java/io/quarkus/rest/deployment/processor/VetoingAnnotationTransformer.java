package io.quarkus.rest.deployment.processor;

import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.rest.deployment.framework.QuarkusRestDotNames;

/**
 * If a JAX-RS resource uses something like @QueryParam, @HeaderParam or the like in a constructor parameter,
 * then we need to make sure that Arc doesn't create a bean for it automatically (as it will fail validation because
 * there is no way to pass the parameter).
 * For these resources we add {@link javax.enterprise.inject.Vetoed}, and we generate custom CDI producers under the hood
 * in {@link CustomResourceProducersGenerator#generate}.
 */
public class VetoingAnnotationTransformer implements AnnotationsTransformer {

    private final Set<DotName> resourcesThatNeedCustomProducer;

    public VetoingAnnotationTransformer(Set<DotName> resourcesThatNeedCustomProducer) {
        this.resourcesThatNeedCustomProducer = resourcesThatNeedCustomProducer;
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return AnnotationTarget.Kind.CLASS == kind;
    }

    @Override
    public void transform(TransformationContext context) {
        if (resourcesThatNeedCustomProducer.contains(context.getTarget().asClass().name())) {
            context.transform().add(QuarkusRestDotNames.VETOED).done();
        }
    }
}
