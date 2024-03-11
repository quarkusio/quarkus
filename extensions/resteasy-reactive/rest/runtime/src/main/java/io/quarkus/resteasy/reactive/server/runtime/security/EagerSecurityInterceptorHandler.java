package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityContext.lazyMethodToMethodDescription;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.vertx.ext.web.RoutingContext;

/**
 * Allows security customizations at the moment when request is matched with resource method, but
 * {@link SecurityCheck}s are yet to be run. This only makes sense when proactive auth is disabled.
 */
public class EagerSecurityInterceptorHandler implements ServerRestHandler {

    private static final Consumer<RoutingContext> NULL_SENTINEL = new Consumer<RoutingContext>() {
        @Override
        public void accept(RoutingContext routingContext) {

        }
    };
    private volatile Consumer<RoutingContext> interceptor;

    private EagerSecurityInterceptorHandler() {
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // right now we do apply security interceptors even when authorization is disabled (for example for tests), as
        // even though you don't want to run security checks, you still might want to authenticate (access identity)

        if (interceptor == NULL_SENTINEL) {
            return;
        }

        if (interceptor == null) {
            MethodDescription methodDescription = lazyMethodToMethodDescription(requestContext.getTarget().getLazyMethod());
            interceptor = Arc.container().select(EagerSecurityInterceptorStorage.class).get().getInterceptor(methodDescription);

            if (interceptor == null) {
                interceptor = NULL_SENTINEL;
                return;
            }
        }

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
                return Collections.singletonList(new EagerSecurityInterceptorHandler());
            }
            return Collections.emptyList();
        }

    }
}
