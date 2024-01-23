package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

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
    private volatile SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper;
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
            SecurityCheckStorage storage = Arc.container().instance(SecurityCheckStorage.class).get();
            check = storage.getSecurityCheck(methodDescription);
            if (check == null) {
                if (storage.getDefaultSecurityCheck() == null || isRequestAlreadyChecked(requestContext)) {
                    check = NULL_SENTINEL;
                } else {
                    check = storage.getDefaultSecurityCheck();
                }
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
            if (getSecurityEventHelper().fireEventOnSuccess()) {
                getSecurityEventHelper().fireSuccessEvent(new AuthorizationSuccessEvent(null, theCheck.getClass().getName(),
                        createEventPropsWithRoutingCtx(requestContext)));
            }
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
                            var unauthorizedException = new UnauthorizedException();
                            if (getSecurityEventHelper().fireEventOnFailure()) {
                                getSecurityEventHelper()
                                        .fireFailureEvent(new AuthorizationFailureEvent(securityIdentity, unauthorizedException,
                                                theCheck.getClass().getName(), createEventPropsWithRoutingCtx(requestContext)));
                            }
                            throw unauthorizedException;
                        }
                        // security check will be performed by CDI interceptor
                        return Uni.createFrom().nullItem();
                    } else {
                        preventRepeatedSecurityChecks(requestContext, methodDescription);
                        var checkResult = theCheck.nonBlockingApply(securityIdentity, methodDescription,
                                requestContext.getParameters());
                        if (getSecurityEventHelper().fireEventOnFailure()) {
                            checkResult = checkResult
                                    .onFailure()
                                    .invoke(new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            getSecurityEventHelper().fireFailureEvent(new AuthorizationFailureEvent(
                                                    securityIdentity, throwable, theCheck.getClass().getName(),
                                                    createEventPropsWithRoutingCtx(requestContext)));
                                        }
                                    });
                        }
                        if (getSecurityEventHelper().fireEventOnSuccess()) {
                            checkResult = checkResult
                                    .invoke(new Runnable() {
                                        @Override
                                        public void run() {
                                            getSecurityEventHelper().fireSuccessEvent(new AuthorizationSuccessEvent(
                                                    securityIdentity, theCheck.getClass().getName(),
                                                    createEventPropsWithRoutingCtx(requestContext)));
                                        }
                                    });
                        }
                        return checkResult;
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

    private static Map<String, Object> createEventPropsWithRoutingCtx(ResteasyReactiveRequestContext requestContext) {
        final RoutingContext routingContext = requestContext.unwrap(RoutingContext.class);
        if (routingContext == null) {
            return Map.of();
        } else {
            return Map.of(RoutingContext.class.getName(), routingContext);
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

    private boolean isRequestAlreadyChecked(ResteasyReactiveRequestContext requestContext) {
        // when request has already been checked at least once (by another instance of this handler)
        // then default security checks, like denied access to all JAX-RS resources by default
        // shouldn't be applied; this doesn't mean security checks registered for methods shouldn't be applied
        return requestContext.getProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR) != null;
    }

    private InjectableInstance<CurrentIdentityAssociation> getCurrentIdentityAssociation() {
        InjectableInstance<CurrentIdentityAssociation> identityAssociation = this.currentIdentityAssociation;
        if (identityAssociation == null) {
            return this.currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class);
        }
        return identityAssociation;
    }

    private SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> getSecurityEventHelper() {
        if (securityEventHelper == null) {
            securityEventHelper = Arc.container().instance(SecurityEventContext.class).get().getHelper();
        }
        return securityEventHelper;
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
