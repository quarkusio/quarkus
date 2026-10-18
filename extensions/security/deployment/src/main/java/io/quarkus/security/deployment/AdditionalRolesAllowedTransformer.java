package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityProcessor.createMethodDescription;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.spi.runtime.MethodDescription;

final class AdditionalRolesAllowedTransformer
        implements Predicate<MethodInfo>, Consumer<AnnotationTransformation.TransformationContext> {

    private static final DotName ROLES_ALLOWED = DotName.createSimple(RolesAllowed.class.getName());
    private final Set<MethodDescription> methods;
    private final AnnotationValue[] rolesAllowed;

    AdditionalRolesAllowedTransformer(Collection<MethodDescription> methods, List<String> rolesAllowed) {
        this.methods = Set.copyOf(methods);
        this.rolesAllowed = rolesAllowed.stream().map(s -> AnnotationValue.createStringValue("", s))
                .toArray(AnnotationValue[]::new);
    }

    @Override
    public boolean test(MethodInfo methodInfo) {
        MethodDescription method = createMethodDescription(methodInfo);
        return methods.contains(method);
    }

    @Override
    public void accept(AnnotationTransformation.TransformationContext ctx) {
        AnnotationInstance rolesAllowedInstance = AnnotationInstance.create(ROLES_ALLOWED, ctx.declaration().asMethod(),
                new AnnotationValue[] { AnnotationValue.createArrayValue("value", rolesAllowed) });
        ctx.add(rolesAllowedInstance);
    }
}
