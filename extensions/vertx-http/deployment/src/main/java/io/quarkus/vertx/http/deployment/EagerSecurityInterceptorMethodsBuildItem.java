package io.quarkus.vertx.http.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;

/**
 * Bears collected intercepted methods annotated with registered security annotation.
 * Security interceptor needs to be created and applied for each intercepted method.
 *
 * @see EagerSecurityInterceptorBindingBuildItem for more information on security filters
 */
public final class EagerSecurityInterceptorMethodsBuildItem extends MultiBuildItem {

    /**
     * Annotation binding value: '@HttpAuthenticationMechanism("custom")' => 'custom'; mapped to annotated methods
     */
    final Map<String, List<MethodInfo>> bindingValueToInterceptedMethods;

    /**
     * Interceptor binding annotation name, like {@link HttpAuthenticationMechanism}.
     */
    final DotName interceptorBinding;

    EagerSecurityInterceptorMethodsBuildItem(Map<String, List<MethodInfo>> bindingValueToInterceptedMethods,
            DotName interceptorBinding) {
        this.bindingValueToInterceptedMethods = Map.copyOf(bindingValueToInterceptedMethods);
        this.interceptorBinding = interceptorBinding;
    }

    private Stream<MethodInfo> interceptedMethods() {
        return bindingValueToInterceptedMethods.values().stream().flatMap(Collection::stream);
    }

    public static List<MethodInfo> collectInterceptedMethods(List<EagerSecurityInterceptorMethodsBuildItem> items) {
        return items.stream().flatMap(EagerSecurityInterceptorMethodsBuildItem::interceptedMethods).toList();
    }

}
