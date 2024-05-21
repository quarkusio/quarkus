package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;
import static io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityContext.lazyMethodToMethodDescription;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
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
    private final boolean onlyCheckForHttpPermissions;
    private volatile SecurityCheck check;

    public EagerSecurityHandler(boolean onlyCheckForHttpPermissions) {
        this.onlyCheckForHttpPermissions = onlyCheckForHttpPermissions;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (!EagerSecurityContext.instance.authorizationController.isAuthorizationEnabled()) {
            return;
        }

        var securityCheck = getSecurityCheck(requestContext);
        final Uni<?> check;
        if (securityCheck == null) {
            if (EagerSecurityContext.instance.doNotRunPermissionSecurityCheck) {
                // no check
                return;
            } else {
                // only permission check
                check = Uni.createFrom().deferred(new Supplier<Uni<?>>() {
                    @Override
                    public Uni<?> get() {
                        return EagerSecurityContext.instance.getPermissionCheck(requestContext, null);
                    }
                });
            }
        } else {
            if (EagerSecurityContext.instance.doNotRunPermissionSecurityCheck) {
                // only security check
                check = EagerSecurityContext.instance.getDeferredIdentity().chain(securityCheck);
            } else {
                // both security check and permission check
                check = EagerSecurityContext.instance.getDeferredIdentity()
                        .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                                return EagerSecurityContext.instance.getPermissionCheck(requestContext, securityIdentity);
                            }
                        })
                        .chain(securityCheck);
            }
        }

        requestContext.requireCDIRequestScope();
        requestContext.suspend();
        check.subscribe().withSubscriber(new UniSubscriber<Object>() {
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

    private Function<SecurityIdentity, Uni<?>> getSecurityCheck(ResteasyReactiveRequestContext requestContext) {
        if (this.onlyCheckForHttpPermissions || this.check == NULL_SENTINEL) {
            return null;
        }
        SecurityCheck check = this.check;
        MethodDescription methodDescription = lazyMethodToMethodDescription(requestContext.getTarget().getLazyMethod());
        if (check == null) {
            check = EagerSecurityContext.instance.securityCheckStorage.getSecurityCheck(methodDescription);
            if (check == null) {
                if (EagerSecurityContext.instance.securityCheckStorage.getDefaultSecurityCheck() == null
                        || isRequestAlreadyChecked(requestContext)) {
                    check = NULL_SENTINEL;
                } else {
                    check = EagerSecurityContext.instance.securityCheckStorage.getDefaultSecurityCheck();
                }
            }
            this.check = check;
        }
        if (check == NULL_SENTINEL) {
            return null;
        }

        if (check.isPermitAll()) {
            preventRepeatedSecurityChecks(requestContext, methodDescription);
            if (EagerSecurityContext.instance.eventHelper.fireEventOnSuccess()) {
                requestContext.requireCDIRequestScope();

                // add the identity only if authentication has already finished
                final SecurityIdentity identity;
                var event = requestContext.unwrap(RoutingContext.class);
                if (event != null && event.user() instanceof QuarkusHttpUser user) {
                    identity = user.getSecurityIdentity();
                } else {
                    identity = null;
                }

                EagerSecurityContext.instance.eventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(identity,
                        check.getClass().getName(), createEventPropsWithRoutingCtx(requestContext), methodDescription));
            }
            return null;
        } else {
            SecurityCheck theCheck = check;
            return new Function<SecurityIdentity, Uni<?>>() {
                @Override
                public Uni<?> apply(SecurityIdentity securityIdentity) {
                    if (EagerSecurityContext.instance.isProactiveAuthDisabled) {
                        // if proactive auth is disabled, then accessing SecurityIdentity would be a blocking
                        // operation if we don't set it; this will allow to access the identity without blocking
                        // from secured endpoints
                        EagerSecurityContext.instance.identityAssociation.get().setIdentity(securityIdentity);
                    }

                    if (theCheck.requiresMethodArguments()) {
                        // if security check requires method arguments, we can't perform it now
                        // however we only allow to pass authenticated requests to avoid security risks
                        if (securityIdentity == null || securityIdentity.isAnonymous()) {
                            var unauthorizedException = new UnauthorizedException();
                            if (EagerSecurityContext.instance.eventHelper.fireEventOnFailure()) {
                                EagerSecurityContext.instance.eventHelper
                                        .fireFailureEvent(new AuthorizationFailureEvent(securityIdentity, unauthorizedException,
                                                theCheck.getClass().getName(), createEventPropsWithRoutingCtx(requestContext),
                                                methodDescription));
                            }
                            throw unauthorizedException;
                        }
                        // security check will be performed by CDI interceptor
                        return Uni.createFrom().nullItem();
                    } else {
                        preventRepeatedSecurityChecks(requestContext, methodDescription);
                        var checkResult = theCheck.nonBlockingApply(securityIdentity, methodDescription,
                                requestContext.getParameters());
                        if (EagerSecurityContext.instance.eventHelper.fireEventOnFailure()) {
                            checkResult = checkResult
                                    .onFailure()
                                    .invoke(new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            EagerSecurityContext.instance.eventHelper
                                                    .fireFailureEvent(new AuthorizationFailureEvent(
                                                            securityIdentity, throwable, theCheck.getClass().getName(),
                                                            createEventPropsWithRoutingCtx(requestContext), methodDescription));
                                        }
                                    });
                        }
                        if (EagerSecurityContext.instance.eventHelper.fireEventOnSuccess()) {
                            checkResult = checkResult
                                    .invoke(new Runnable() {
                                        @Override
                                        public void run() {
                                            EagerSecurityContext.instance.eventHelper.fireSuccessEvent(
                                                    new AuthorizationSuccessEvent(securityIdentity,
                                                            theCheck.getClass().getName(),
                                                            createEventPropsWithRoutingCtx(requestContext), methodDescription));
                                        }
                                    });
                        }
                        return checkResult;
                    }
                }
            };
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

    private static void preventRepeatedSecurityChecks(ResteasyReactiveRequestContext requestContext,
            MethodDescription methodDescription) {
        // propagate information that security check has been performed on this method to the SecurityHandler
        // via io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor
        requestContext.setProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR, methodDescription);
    }

    private static boolean isRequestAlreadyChecked(ResteasyReactiveRequestContext requestContext) {
        // when request has already been checked at least once (by another instance of this handler)
        // then default security checks, like denied access to all JAX-RS resources by default
        // shouldn't be applied; this doesn't mean security checks registered for methods shouldn't be applied
        return requestContext.getProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR) != null;
    }

    public static abstract class Customizer implements HandlerChainCustomizer {

        public static HandlerChainCustomizer newInstance(boolean onlyCheckForHttpPermissions) {
            return onlyCheckForHttpPermissions ? new HttpPermissionsOnlyCustomizer()
                    : new HttpPermissionsAndSecurityChecksCustomizer();
        }

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                return Collections.singletonList(new EagerSecurityHandler(onlyCheckForHttpPermissions()));
            }
            return Collections.emptyList();
        }

        protected abstract boolean onlyCheckForHttpPermissions();

        public static final class HttpPermissionsOnlyCustomizer extends Customizer {

            @Override
            protected boolean onlyCheckForHttpPermissions() {
                return true;
            }
        }

        public static final class HttpPermissionsAndSecurityChecksCustomizer extends Customizer {

            @Override
            protected boolean onlyCheckForHttpPermissions() {
                return false;
            }
        }

    }
}
