package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class EagerSecurityHandler implements ServerRestHandler {

    private static final SecurityCheck NULL_SENTINEL = new SecurityCheck() {
        @Override
        public void apply(SecurityIdentity identity, Method method, Object[] parameters) {

        }

        @Override
        public void apply(SecurityIdentity identity, MethodDescription method, Object[] parameters) {

        }
    };

    private final boolean isProactiveAuthDisabled;
    private volatile InjectableInstance<CurrentIdentityAssociation> currentIdentityAssociation;
    private volatile SecurityCheck check;
    private volatile AuthorizationController authorizationController;

    public EagerSecurityHandler(boolean isProactiveAuthDisabled) {
        this.isProactiveAuthDisabled = isProactiveAuthDisabled;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (this.check == NULL_SENTINEL) {
            return;
        }
        SecurityCheck check = this.check;
        ResteasyReactiveResourceInfo lazyMethod = requestContext.getTarget().getLazyMethod();
        MethodDescription methodDescription = lazyMethodToMethodDescription(lazyMethod);
        if (check == null) {
            check = Arc.container().instance(SecurityCheckStorage.class).get().getSecurityCheck(methodDescription);
            if (check == null) {
                check = NULL_SENTINEL;
            }
            this.check = check;
        }
        if (check == NULL_SENTINEL) {
            return;
        }
        if (authorizationController == null) {
            authorizationController = Arc.container().instance(AuthorizationController.class).get();
        }
        if (!authorizationController.isAuthorizationEnabled()) {
            return;
        }

        requestContext.requireCDIRequestScope();
        SecurityCheck theCheck = check;
        if (theCheck.isPermitAll()) {
            preventRepeatedSecurityChecks(requestContext, methodDescription);
        } else {
            requestContext.suspend();
            Uni<SecurityIdentity> deferredIdentity = getCurrentIdentityAssociation().get().getDeferredIdentity();

            // if proactive auth is disabled, then accessing SecurityIdentity is a blocking operation for synchronous methods
            // setting identity here will enable SecurityInterceptors registered in Quarkus Security Deployment to run checks
            if (isProactiveAuthDisabled && lazyMethod.isNonBlocking) {
                deferredIdentity = deferredIdentity.call(securityIdentity -> {
                    if (securityIdentity != null) {
                        getCurrentIdentityAssociation().get().setIdentity(securityIdentity);
                    }
                    return Uni.createFrom().item(securityIdentity);
                });
            }

            deferredIdentity.flatMap(new Function<SecurityIdentity, Uni<?>>() {
                @Override
                public Uni<?> apply(SecurityIdentity securityIdentity) {
                    if (theCheck.requiresMethodArguments()) {
                        // if security check requires method arguments, we can't perform it now
                        // however we only allow to pass authenticated requests to avoid security risks
                        if (securityIdentity.isAnonymous()) {
                            throw new UnauthorizedException();
                        }
                        // security check will be performed by CDI interceptor
                        return Uni.createFrom().nullItem();
                    } else {
                        preventRepeatedSecurityChecks(requestContext, methodDescription);
                        return theCheck.nonBlockingApply(securityIdentity, methodDescription, requestContext.getParameters());
                    }
                }
            })
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

    static MethodDescription lazyMethodToMethodDescription(ResteasyReactiveResourceInfo lazyMethod) {
        return new MethodDescription(lazyMethod.getResourceClass().getName(),
                lazyMethod.getName(), MethodDescription.typesAsStrings(lazyMethod.getParameterTypes()));
    }

    private void preventRepeatedSecurityChecks(ResteasyReactiveRequestContext requestContext,
            MethodDescription methodDescription) {
        // propagate information that security check has been performed on this method to the SecurityHandler
        // via io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor
        requestContext.setProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR, methodDescription);
    }

    private InjectableInstance<CurrentIdentityAssociation> getCurrentIdentityAssociation() {
        InjectableInstance<CurrentIdentityAssociation> identityAssociation = this.currentIdentityAssociation;
        if (identityAssociation == null) {
            return this.currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class);
        }
        return identityAssociation;
    }

    public static abstract class Customizer implements HandlerChainCustomizer {

        public static HandlerChainCustomizer newInstance(boolean isProactiveAuthEnabled) {
            return isProactiveAuthEnabled ? new ProactiveAuthEnabledCustomizer() : new ProactiveAuthDisabledCustomizer();
        }

        protected abstract boolean isProactiveAuthDisabled();

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                return Collections.singletonList(new EagerSecurityHandler(isProactiveAuthDisabled()));
            }
            return Collections.emptyList();
        }

        public static class ProactiveAuthEnabledCustomizer extends Customizer {

            @Override
            protected boolean isProactiveAuthDisabled() {
                return false;
            }
        }

        public static class ProactiveAuthDisabledCustomizer extends Customizer {

            @Override
            protected boolean isProactiveAuthDisabled() {
                return true;
            }
        }

    }
}
