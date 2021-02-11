package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.DENY_ALL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class AdditionalDenyingUnannotatedTransformer implements AnnotationsTransformer {

    private final Set<String> classNames;

    public AdditionalDenyingUnannotatedTransformer(Collection<String> classNames) {
        this.classNames = new HashSet<>(classNames);
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext context) {
        String className = context.getTarget().asClass().name().toString();
        if (classNames.contains(className)) {
            context.transform().add(DENY_ALL).done();
        }
    }
}
