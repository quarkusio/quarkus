package io.quarkus.vertx.http.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Register {@link HttpAuthenticationMechanism} meta annotations.
 * This way, users can use {@link BasicAuthentication} instead of '@HttpAuthenticationMechanism("basic")'.
 */
public final class HttpAuthMechanismAnnotationBuildItem extends MultiBuildItem {

    /**
     * Annotation name, for example {@link BasicAuthentication}.
     */
    final DotName annotationName;
    /**
     * Authentication mechanism scheme, as defined by {@link HttpAuthenticationMechanism#value()}.
     */
    final String authMechanismScheme;
    /**
     * Classes annotated with {@link #annotationName} excluded from additional security checks.
     * In other words, we do not register {@link io.quarkus.security.Authenticated} security check
     * for these interfaces when no other standard security annotation is not present.
     */
    private final Set<DotName> excludedTargetInterfaces;

    public HttpAuthMechanismAnnotationBuildItem(DotName annotationName, String authMechanismScheme) {
        this.annotationName = Objects.requireNonNull(annotationName);
        this.authMechanismScheme = Objects.requireNonNull(authMechanismScheme);
        this.excludedTargetInterfaces = Set.of();
    }

    public HttpAuthMechanismAnnotationBuildItem(DotName annotationName, String authMechanismScheme,
            DotName... excludedTargetInterfaces) {
        this.annotationName = Objects.requireNonNull(annotationName);
        this.authMechanismScheme = Objects.requireNonNull(authMechanismScheme);
        this.excludedTargetInterfaces = Set.of(Objects.requireNonNull(excludedTargetInterfaces));
    }

    static Predicate<AnnotationTarget> isExcludedAnnotationTarget(List<HttpAuthMechanismAnnotationBuildItem> items) {
        final Set<DotName> excludedInterfaceNames = items.stream().map(i -> i.excludedTargetInterfaces)
                .flatMap(Collection::stream).collect(Collectors.toSet());
        return annotationTarget -> {
            final ClassInfo classInfo;
            if (annotationTarget.kind() == AnnotationTarget.Kind.CLASS) {
                classInfo = annotationTarget.asClass();
            } else if (annotationTarget.kind() == AnnotationTarget.Kind.METHOD
                    && annotationTarget.asMethod().declaringClass() != null) {
                classInfo = annotationTarget.asMethod().declaringClass();
            } else {
                return false;
            }
            return classInfo.interfaceNames().stream().anyMatch(excludedInterfaceNames::contains);
        };
    }
}
