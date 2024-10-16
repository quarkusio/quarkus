package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityProcessor.createMethodDescription;
import static io.quarkus.security.spi.SecurityTransformerUtils.DENY_ALL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.security.spi.runtime.MethodDescription;

public class AdditionalDenyingUnannotatedTransformer implements AnnotationsTransformer {

    private final Set<MethodDescription> methods;

    public AdditionalDenyingUnannotatedTransformer(Collection<MethodDescription> methods) {
        this.methods = new HashSet<>(methods);
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.METHOD;
    }

    @Override
    public void transform(TransformationContext context) {
        MethodDescription methodDescription = createMethodDescription(context.getTarget().asMethod());
        if (methods.contains(methodDescription)) {
            context.transform().add(DENY_ALL).done();
        }
    }
}
