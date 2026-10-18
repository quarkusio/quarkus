package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityProcessor.createMethodDescription;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.annotation.security.DenyAll;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.spi.runtime.MethodDescription;

final class AdditionalDenyingUnannotatedTransformer
        implements Predicate<MethodInfo>, Consumer<AnnotationTransformation.TransformationContext> {

    private final Set<MethodDescription> methods;

    AdditionalDenyingUnannotatedTransformer(Collection<MethodDescription> methods) {
        this.methods = Set.copyOf(methods);
    }

    @Override
    public void accept(AnnotationTransformation.TransformationContext ctx) {
        ctx.add(DenyAll.class);
    }

    @Override
    public boolean test(MethodInfo methodInfo) {
        MethodDescription methodDescription = createMethodDescription(methodInfo);
        return methods.contains(methodDescription);
    }
}
