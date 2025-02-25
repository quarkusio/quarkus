package io.quarkus.vertx.http.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Bears collected intercepted classes annotated with registered security annotation
 * if and only if class-level security is applied due
 * to the matching {@link io.quarkus.security.spi.ClassSecurityAnnotationBuildItem} annotation.
 * Security interceptor needs to be created and applied for each intercepted class.
 *
 * @see EagerSecurityInterceptorBindingBuildItem for more information on security filters
 */
public final class EagerSecurityInterceptorClassesBuildItem extends MultiBuildItem {

    /**
     * Annotation binding value: '@HttpAuthenticationMechanism("custom")' => 'custom'; mapped to annotated class names
     */
    final Map<String, Set<String>> bindingValueToInterceptedClasses;

    /**
     * Interceptor binding annotation name, like {@link HttpAuthenticationMechanism}.
     */
    final DotName interceptorBinding;

    EagerSecurityInterceptorClassesBuildItem(Map<String, Set<String>> bindingValueToInterceptedClasses,
            DotName interceptorBinding) {
        this.bindingValueToInterceptedClasses = Map.copyOf(bindingValueToInterceptedClasses);
        this.interceptorBinding = interceptorBinding;
    }

    public static Set<String> collectInterceptedClasses(List<EagerSecurityInterceptorClassesBuildItem> items) {
        return items.stream()
                .map(i -> i.bindingValueToInterceptedClasses)
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
