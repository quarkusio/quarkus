package io.quarkus.resteasy.reactive.server.runtime.security;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.vertx.ext.web.RoutingContext;

/**
 * Allows security customizations at the moment when request is matched with resource method, but
 * {@link SecurityCheck}s are yet to be run. This only makes sense when proactive auth is disabled.
 */
public class EagerSecurityInterceptorHandler implements ServerRestHandler {

    private final Consumer<RoutingContext> interceptor;

    private EagerSecurityInterceptorHandler(Consumer<RoutingContext> interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // right now we do apply security interceptors even when authorization is disabled (for example for tests), as
        // even though you don't want to run security checks, you still might want to authenticate (access identity)
        interceptor.accept(requestContext.unwrap(RoutingContext.class));
    }

    public static class Customizer implements HandlerChainCustomizer {

        public static HandlerChainCustomizer newInstance() {
            return new Customizer();
        }

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {

                var desc = ResourceMethodDescription.of(serverResourceMethod);
                var interceptorStorage = Arc.container().instance(EagerSecurityInterceptorStorage.class).get();
                var interceptor = interceptorStorage.getInterceptor(desc.invokedMethodDesc());
                if (interceptor == null && desc.fallbackMethodDesc() != null) {
                    interceptor = interceptorStorage.getInterceptor(desc.fallbackMethodDesc());
                }

                if (interceptor == null) {
                    throw new IllegalStateException(
                            """
                                    Security annotation placed on resource method '%s#%s' wasn't detected by Quarkus during the build time.
                                    Please consult https://quarkus.io/guides/cdi-reference#bean_discovery on how to make the module containing the code discoverable by Quarkus.
                                    """
                                    .formatted(desc.invokedMethodDesc().getClassName(),
                                            desc.invokedMethodDesc().getMethodName()));
                }

                return Collections.singletonList(new EagerSecurityInterceptorHandler(interceptor));
            }
            return Collections.emptyList();
        }

    }
}
