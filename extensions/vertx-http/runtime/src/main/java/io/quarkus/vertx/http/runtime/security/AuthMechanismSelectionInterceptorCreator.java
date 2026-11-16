package io.quarkus.vertx.http.runtime.security;

import java.util.function.Consumer;
import java.util.function.Function;

import io.vertx.ext.web.RoutingContext;

/**
 * Creates interceptor that selects authentication mechanisms based
 * on the {@link io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism} annotation.
 */
public final class AuthMechanismSelectionInterceptorCreator implements Function<String, Consumer<RoutingContext>> {

    @Override
    public Consumer<RoutingContext> apply(String authMechanismName) {
        // when endpoint is annotated with @HttpAuthenticationMechanism("my-mechanism"), we add this mechanism
        // to the event so that when request is being authenticated, the HTTP authenticator will know
        // what mechanism should be used
        return new Consumer<RoutingContext>() {
            @Override
            public void accept(RoutingContext routingContext) {
                HttpAuthenticator.selectAuthMechanism(routingContext, authMechanismName);
            }
        };
    }

}
