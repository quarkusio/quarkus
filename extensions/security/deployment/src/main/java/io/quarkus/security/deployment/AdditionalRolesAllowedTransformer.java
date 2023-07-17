package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityProcessor.createMethodDescription;
import static io.quarkus.security.deployment.SecurityTransformerUtils.ROLES_ALLOWED;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.security.spi.runtime.MethodDescription;

public class AdditionalRolesAllowedTransformer implements AnnotationsTransformer {

    private final Set<MethodDescription> methods;
    private final AnnotationValue[] rolesAllowed;

    public AdditionalRolesAllowedTransformer(Collection<MethodDescription> methods, List<String> rolesAllowed) {
        this.methods = new HashSet<>(methods);
        this.rolesAllowed = rolesAllowed.stream().map(s -> AnnotationValue.createStringValue("", s))
                .toArray(AnnotationValue[]::new);
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.METHOD;
    }

    @Override
    public void transform(TransformationContext context) {
        MethodDescription method = createMethodDescription(context.getTarget().asMethod());
        if (methods.contains(method)) {
            context.transform().add(ROLES_ALLOWED, AnnotationValue.createArrayValue("value", rolesAllowed)).done();
        }
    }
}
