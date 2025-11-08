package io.quarkus.security.deployment;

import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.spi.SecurityTransformer;

final class DenyUnannotatedPredicate implements Predicate<ClassInfo> {

    private final SecurityTransformer securityTransformer;

    DenyUnannotatedPredicate(SecurityTransformer securityTransformer) {
        this.securityTransformer = securityTransformer;
    }

    @Override
    public boolean test(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        return !securityTransformer.hasSecurityAnnotation(classInfo)
                && methods.stream().anyMatch(securityTransformer::hasSecurityAnnotation);
    }
}
