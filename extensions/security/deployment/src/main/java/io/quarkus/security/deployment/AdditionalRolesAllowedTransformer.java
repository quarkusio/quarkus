package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.ROLES_ALLOWED;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.arc.processor.AnnotationsTransformer;

public class AdditionalRolesAllowedTransformer implements AnnotationsTransformer {

    private final Set<String> classNames;
    private final AnnotationValue[] rolesAllowed;

    public AdditionalRolesAllowedTransformer(Collection<String> classNames, List<String> rolesAllowed) {
        this.classNames = new HashSet<>(classNames);
        this.rolesAllowed = rolesAllowed.stream().map(s -> AnnotationValue.createStringValue("", s))
                .toArray(AnnotationValue[]::new);
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext context) {
        String className = context.getTarget().asClass().name().toString();
        if (classNames.contains(className)) {
            context.transform().add(ROLES_ALLOWED, AnnotationValue.createArrayValue("value", rolesAllowed)).done();
        }
    }
}
