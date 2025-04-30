package io.quarkus.vertx.http.deployment;

import java.util.Collection;
import java.util.HashMap;
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

    /**
     * If this interceptor is always accompanied by {@link io.quarkus.security.spi.runtime.SecurityCheck}.
     * For example, we know that endpoint annotated with {@link HttpAuthenticationMechanism} is always secured.
     */
    private final boolean requiresSecurityCheck;

    EagerSecurityInterceptorMethodsBuildItem(Map<String, List<MethodInfo>> bindingValueToInterceptedMethods,
            DotName interceptorBinding, boolean requiresSecurityCheck) {
        this.bindingValueToInterceptedMethods = Map.copyOf(bindingValueToInterceptedMethods);
        this.interceptorBinding = interceptorBinding;
        this.requiresSecurityCheck = requiresSecurityCheck;
    }

    private Stream<MethodInfo> interceptedMethods() {
        return bindingValueToInterceptedMethods.values().stream().flatMap(Collection::stream);
    }

    public static Map<MethodInfo, Boolean> collectInterceptedMethods(List<EagerSecurityInterceptorMethodsBuildItem> items) {
        Map<MethodInfo, Boolean> result = new HashMap<>();
        for (var item : items) {
            item.interceptedMethods().forEach(mi -> {
                if (result.containsKey(mi)) {
                    var requiresCheck = result.get(mi);
                    if (!requiresCheck && item.requiresSecurityCheck) {
                        result.put(mi, true);
                    }
                } else {
                    result.put(mi, item.requiresSecurityCheck);
                }
            });
        }
        return result;
    }
}
