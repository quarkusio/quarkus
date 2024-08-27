package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;

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

import io.quarkus.arc.Arc;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.quarkus.vertx.http.runtime.security.AuthorizationPolicyStorage;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

public class EagerSecurityHandler implements ServerRestHandler {

    /**
     * Used when no endpoint security checks were detected, no default Jakarta REST security is in place, and
     * we have this handler in place for whether Jakarta REST specific HTTP Permissions are required
     * is determined when runtime config is available.
     */
    private static final EagerSecurityHandler HTTP_PERMS_ONLY = new EagerSecurityHandler(null, false, null);

    private final SecurityCheck check;
    private final boolean isDefaultJaxRsSecCheck;
    private final MethodDescription invokedMethodDesc;

    private EagerSecurityHandler(SecurityCheck check, boolean isDefaultJaxRsSecCheck, MethodDescription invokedMethodDesc) {
        this.check = check;
        this.isDefaultJaxRsSecCheck = isDefaultJaxRsSecCheck;
        this.invokedMethodDesc = invokedMethodDesc;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (!EagerSecurityContext.instance.authorizationController.isAuthorizationEnabled()) {
            return;
        }

        if (isDefaultJaxRsSecCheck && isRequestAlreadyChecked(requestContext)) {
            // default Jakarta REST security is applied on subresource locators
            // this ensures it's not reapplied on subresource endpoints
            return;
        }

        final Function<SecurityIdentity, Uni<?>> checkRequiringIdentity;
        if (check == null) {
            checkRequiringIdentity = null;
        } else {
            checkRequiringIdentity = getSecurityCheck(requestContext, check, invokedMethodDesc);
        }

        final Uni<?> check;
        if (checkRequiringIdentity == null) {
            if (EagerSecurityContext.instance.doNotRunPermissionSecurityCheck) {
                // either permit all security check or no check at all
                return;
            } else {
                // only HTTP permission check
                check = Uni.createFrom().deferred(new Supplier<Uni<?>>() {
                    @Override
                    public Uni<?> get() {
                        return EagerSecurityContext.instance.getPermissionCheck(requestContext, null, invokedMethodDesc);
                    }
                });
            }
        } else {
            if (EagerSecurityContext.instance.doNotRunPermissionSecurityCheck) {
                // only security check that requires identity
                check = EagerSecurityContext.instance.getDeferredIdentity().chain(checkRequiringIdentity);
            } else {
                // both security check that requires identity and HTTP permission check
                check = EagerSecurityContext.instance.getDeferredIdentity()
                        .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                                return EagerSecurityContext.instance.getPermissionCheck(requestContext, securityIdentity,
                                        invokedMethodDesc);
                            }
                        })
                        .chain(checkRequiringIdentity);
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

    /**
     * @return null if the check permits all requests, otherwise fun that requires identity to perform check
     */
    private static Function<SecurityIdentity, Uni<?>> getSecurityCheck(ResteasyReactiveRequestContext requestContext,
            SecurityCheck check, MethodDescription invokedMethodDesc) {
        if (check.isPermitAll()) {
            preventRepeatedSecurityChecks(requestContext, invokedMethodDesc);
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
                        check.getClass().getName(), createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
            }
            return null;
        } else {
            return new Function<SecurityIdentity, Uni<?>>() {
                @Override
                public Uni<?> apply(SecurityIdentity securityIdentity) {
                    if (EagerSecurityContext.instance.isProactiveAuthDisabled) {
                        // if proactive auth is disabled, then accessing SecurityIdentity would be a blocking
                        // operation if we don't set it; this will allow to access the identity without blocking
                        // from secured endpoints
                        EagerSecurityContext.instance.identityAssociation.get().setIdentity(securityIdentity);
                    }

                    if (check.requiresMethodArguments()) {
                        // if security check requires method arguments, we can't perform it now
                        // however we only allow to pass authenticated requests to avoid security risks
                        if (securityIdentity == null || securityIdentity.isAnonymous()) {
                            var unauthorizedException = new UnauthorizedException();
                            if (EagerSecurityContext.instance.eventHelper.fireEventOnFailure()) {
                                EagerSecurityContext.instance.eventHelper
                                        .fireFailureEvent(new AuthorizationFailureEvent(securityIdentity, unauthorizedException,
                                                check.getClass().getName(), createEventPropsWithRoutingCtx(requestContext),
                                                invokedMethodDesc));
                            }
                            throw unauthorizedException;
                        }
                        // security check will be performed by CDI interceptor
                        return Uni.createFrom().nullItem();
                    } else {
                        preventRepeatedSecurityChecks(requestContext, invokedMethodDesc);
                        var checkResult = check.nonBlockingApply(securityIdentity, invokedMethodDesc,
                                requestContext.getParameters());
                        if (EagerSecurityContext.instance.eventHelper.fireEventOnFailure()) {
                            checkResult = checkResult
                                    .onFailure()
                                    .invoke(new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            EagerSecurityContext.instance.eventHelper
                                                    .fireFailureEvent(new AuthorizationFailureEvent(
                                                            securityIdentity, throwable, check.getClass().getName(),
                                                            createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
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
                                                            check.getClass().getName(),
                                                            createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
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

        public static HandlerChainCustomizer newInstanceWithAuthorizationPolicy() {
            return new AuthZPolicyCustomizer();
        }

        public static HandlerChainCustomizer newInstance(boolean onlyCheckForHttpPermissions) {
            return onlyCheckForHttpPermissions ? new HttpPermissionsOnlyCustomizer()
                    : new HttpPermissionsAndSecurityChecksCustomizer();
        }

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                if (onlyCheckForHttpPermissions()) {
                    if (applyAuthorizationPolicy()) {
                        return createHandlerForAuthZPolicy(serverResourceMethod);
                    }
                    return Collections.singletonList(HTTP_PERMS_ONLY);
                }

                boolean isDefaultJaxRsSecCheck = false;
                var desc = ResourceMethodDescription.of(serverResourceMethod);
                var checkStorage = Arc.container().instance(SecurityCheckStorage.class).get();

                var check = checkStorage.getSecurityCheck(desc.invokedMethodDesc());
                if (check == null && desc.fallbackMethodDesc() != null) {
                    check = checkStorage.getSecurityCheck(desc.fallbackMethodDesc());
                }
                if (check == null) {
                    check = checkStorage.getDefaultSecurityCheck();
                    isDefaultJaxRsSecCheck = true;
                }

                if (check == null) {
                    throw new IllegalStateException(
                            """
                                    Security annotation placed on resource method '%s#%s' wasn't detected by Quarkus during the build time.
                                    Please consult https://quarkus.io/guides/cdi-reference#bean_discovery on how to make the module containing the code discoverable by Quarkus.
                                    """
                                    .formatted(desc.invokedMethodDesc().getClassName(),
                                            desc.invokedMethodDesc().getMethodName()));
                }

                return Collections
                        .singletonList(new EagerSecurityHandler(check, isDefaultJaxRsSecCheck, desc.invokedMethodDesc()));
            }
            return Collections.emptyList();
        }

        private static List<ServerRestHandler> createHandlerForAuthZPolicy(ServerResourceMethod serverResourceMethod) {
            var desc = ResourceMethodDescription.of(serverResourceMethod);
            var authorizationPolicyStorage = Arc.container().select(AuthorizationPolicyStorage.class).get();
            final MethodDescription securedMethod;
            if (authorizationPolicyStorage.requiresAuthorizationPolicy(desc.invokedMethodDesc())) {
                securedMethod = desc.invokedMethodDesc();
            } else if (authorizationPolicyStorage.requiresAuthorizationPolicy(desc.fallbackMethodDesc())) {
                securedMethod = desc.fallbackMethodDesc();
            } else {
                throw new IllegalStateException(
                        """
                                @AuthorizationPolicy annotation placed on resource method '%s#%s' wasn't detected by Quarkus during the build time.
                                Please consult https://quarkus.io/guides/cdi-reference#bean_discovery on how to make the module containing the code discoverable by Quarkus.
                                """
                                .formatted(desc.invokedMethodDesc().getClassName(),
                                        desc.invokedMethodDesc().getMethodName()));
            }
            return Collections.singletonList(new EagerSecurityHandler(null, false, securedMethod));
        }

        protected abstract boolean onlyCheckForHttpPermissions();

        protected abstract boolean applyAuthorizationPolicy();

        public static final class HttpPermissionsOnlyCustomizer extends Customizer {

            @Override
            protected boolean onlyCheckForHttpPermissions() {
                return true;
            }

            @Override
            protected boolean applyAuthorizationPolicy() {
                return false;
            }
        }

        public static final class AuthZPolicyCustomizer extends Customizer {

            @Override
            protected boolean onlyCheckForHttpPermissions() {
                return true;
            }

            @Override
            protected boolean applyAuthorizationPolicy() {
                return true;
            }
        }

        public static final class HttpPermissionsAndSecurityChecksCustomizer extends Customizer {

            @Override
            protected boolean onlyCheckForHttpPermissions() {
                return false;
            }

            @Override
            protected boolean applyAuthorizationPolicy() {
                return false;
            }
        }

    }
}
