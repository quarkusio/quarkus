package io.quarkus.resteasy.reactive.server.runtime.security;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Runs {@link SecurityCheck} for endpoint methods when {@link SecurityCheck#requiresMethodArguments()} is true.
 */
final class SecurityCheckWithMethodArgsHandler implements ServerRestHandler {

    private final SecurityCheck securityCheck;
    private final MethodDescription invokedMethodDesc;

    SecurityCheckWithMethodArgsHandler(SecurityCheck securityCheck, MethodDescription invokedMethodDesc) {
        this.securityCheck = securityCheck;
        this.invokedMethodDesc = invokedMethodDesc;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        if (!EagerSecurityContext.isAuthorizationEnabled()) {
            return;
        }

        requestContext.requireCDIRequestScope();
        requestContext.suspend();

        // no need to use deferred identity as we require request to be authenticated on pre-match and set identity
        SecurityIdentity securityIdentity = EagerSecurityContext.getCurrentIdentityAssociation().getIdentity();
        EagerSecurityContext.getInstance()
                .runSecurityCheck(securityCheck, invokedMethodDesc, requestContext, securityIdentity)
                .subscribe().withSubscriber(new UniSubscriber<Object>() {
                    @Override
                    public void onSubscribe(UniSubscription subscription) {

                    }

                    @Override
                    public void onItem(Object item) {
                        requestContext.resume();
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        requestContext.resume(failure, true);
                    }
                });
    }
}
