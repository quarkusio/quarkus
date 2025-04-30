package io.quarkus.security.deployment;

import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.spi.SecurityTransformerUtils;

final class DenyUnannotatedPredicate implements Predicate<ClassInfo> {

    @Override
    public boolean test(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        return !SecurityTransformerUtils.hasSecurityAnnotation(classInfo)
                && methods.stream().anyMatch(SecurityTransformerUtils::hasSecurityAnnotation);
    }
}
