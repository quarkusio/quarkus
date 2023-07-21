package io.quarkus.vertx.http.deployment;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.vertx.ext.web.RoutingContext;

/**
 * Vert.X route handlers run before REST layer can't determine which endpoint is going to be invoked,
 * what are endpoint annotations etc. Therefore, security setting that requires knowledge of invoked method
 * (initial intention is to provide this with RESTEasy Reactive resources, however the principle is applicable to
 * other stacks as well) and needs to be run prior to any security check should use this build item. The build
 * item is only required for stacks that do not run security checks via CDI interceptors, as there, you can simply
 * use interceptor with higher priority.
 */
public final class EagerSecurityInterceptorCandidateBuildItem extends MultiBuildItem {

    private final MethodInfo methodInfo;
    private final RuntimeValue<MethodDescription> descriptionRuntimeValue;
    private final Consumer<RoutingContext> securityInterceptor;

    /**
     * @param methodInfo endpoint candidate; extensions exposing endpoints has final say on what is endpoint
     * @param descriptionRuntimeValue endpoint candidate transformed into description
     * @param securityInterceptor piece of code that should be run before {@link io.quarkus.security.spi.runtime.SecurityCheck}
     *        for annotated method is invoked; must be recorded during static init
     */
    public EagerSecurityInterceptorCandidateBuildItem(MethodInfo methodInfo,
            RuntimeValue<MethodDescription> descriptionRuntimeValue,
            Consumer<RoutingContext> securityInterceptor) {
        this.methodInfo = Objects.requireNonNull(methodInfo);
        this.descriptionRuntimeValue = Objects.requireNonNull(descriptionRuntimeValue);
        this.securityInterceptor = securityInterceptor;
    }

    public static boolean hasProperEndpointModifiers(MethodInfo info) {
        // synthetic methods are not endpoints
        if ((info.flags() & 0x1000) != 0) {
            return false;
        }
        // public only
        if ((info.flags() & Modifier.PUBLIC) == 0) {
            return false;
        }
        // instance methods only
        return (info.flags() & Modifier.STATIC) == 0;
    }

    MethodInfo getMethodInfo() {
        return methodInfo;
    }

    RuntimeValue<MethodDescription> getDescriptionRuntimeValue() {
        return descriptionRuntimeValue;
    }

    Consumer<RoutingContext> getSecurityInterceptor() {
        return securityInterceptor;
    }
}
