package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;
import static io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityContext.createEventPropsWithRoutingCtx;
import static io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityContext.preventRepeatedSecurityChecks;

import java.util.List;
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
        if (!EagerSecurityContext.isAuthorizationEnabled()) {
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
            if (EagerSecurityContext.doNotRunPermissionSecurityCheck()) {
                // either permit all security check or no check at all
                return;
            } else {
                // only HTTP permission check
                check = Uni.createFrom().deferred(new Supplier<Uni<?>>() {
                    @Override
                    public Uni<?> get() {
                        return EagerSecurityContext.getInstance().getPermissionCheck(requestContext, null, invokedMethodDesc);
                    }
                });
            }
        } else {
            if (EagerSecurityContext.doNotRunPermissionSecurityCheck()) {
                // only security check that requires identity
                check = EagerSecurityContext.getInstance().getDeferredIdentity().chain(checkRequiringIdentity);
            } else {
                // both security check that requires identity and HTTP permission check
                check = EagerSecurityContext.getInstance().getDeferredIdentity()
                        .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                                return EagerSecurityContext.getInstance().getPermissionCheck(requestContext, securityIdentity,
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
            if (EagerSecurityContext.getEventHelper().fireEventOnSuccess()) {
                requestContext.requireCDIRequestScope();

                // add the identity only if authentication has already finished
                final SecurityIdentity identity;
                var event = requestContext.unwrap(RoutingContext.class);
                if (event != null && event.user() instanceof QuarkusHttpUser user) {
                    identity = user.getSecurityIdentity();
                } else {
                    identity = null;
                }

                EagerSecurityContext.getEventHelper().fireSuccessEvent(new AuthorizationSuccessEvent(identity,
                        check.getClass().getName(), createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
            }
            return null;
        } else {
            return new Function<SecurityIdentity, Uni<?>>() {
                @Override
                public Uni<?> apply(SecurityIdentity securityIdentity) {
                    if (EagerSecurityContext.isProactiveAuthDisabled()) {
                        // if proactive auth is disabled, then accessing SecurityIdentity would be a blocking
                        // operation if we don't set it; this will allow to access the identity without blocking
                        // from secured endpoints
                        EagerSecurityContext.getCurrentIdentityAssociation().setIdentity(securityIdentity);
                    }

                    if (check.requiresMethodArguments()) {
                        // if security check requires method arguments, we can't perform it now
                        // however we only allow to pass authenticated requests to avoid security risks
                        if (securityIdentity == null || securityIdentity.isAnonymous()) {
                            var unauthorizedException = new UnauthorizedException();
                            if (EagerSecurityContext.getEventHelper().fireEventOnFailure()) {
                                EagerSecurityContext.getEventHelper()
                                        .fireFailureEvent(new AuthorizationFailureEvent(securityIdentity, unauthorizedException,
                                                check.getClass().getName(), createEventPropsWithRoutingCtx(requestContext),
                                                invokedMethodDesc));
                            }
                            throw unauthorizedException;
                        }
                        // security check will be performed by CDI interceptor
                        return Uni.createFrom().nullItem();
                    } else {
                        return EagerSecurityContext.getInstance().runSecurityCheck(check, invokedMethodDesc, requestContext,
                                securityIdentity);
                    }
                }
            };
        }
    }

    private static boolean isRequestAlreadyChecked(ResteasyReactiveRequestContext requestContext) {
        // when request has already been checked at least once (by another instance of this handler)
        // then default security checks, like denied access to all JAX-RS resources by default
        // shouldn't be applied; this doesn't mean security checks registered for methods shouldn't be applied
        return requestContext.getProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR) != null;
    }

    public static final class HttpPermissionsOnlyCustomizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                return List.of(HTTP_PERMS_ONLY);
            }
            return List.of();
        }
    }

    public static final class AuthZPolicyCustomizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
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
                return List.of(new EagerSecurityHandler(null, false, securedMethod));
            }
            return List.of();
        }
    }

    public static final class HttpPermissionsAndSecurityChecksCustomizer implements HandlerChainCustomizer {

        private volatile SecurityCheckInfo securityCheckInfo;

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                final SecurityCheckInfo info = getSecurityCheckInfo(serverResourceMethod);
                return List.of(new EagerSecurityHandler(info.check, info.isDefaultJaxRsSecCheck, info.invokedMethodDesc));
            }

            if (phase == Phase.BEFORE_METHOD_INVOKE && requiresMethodArguments(serverResourceMethod)) {
                final SecurityCheckInfo info = getSecurityCheckInfo(serverResourceMethod);
                if (info.isDefaultJaxRsSecCheck) {
                    // with current implementation, this IF will never be true as the default checks are about
                    // default @RolesAllowed or @Deny configurable in application.properties for unannotated methods;
                    // it is difficult to imagine check that requires method arguments and is applied for all methods;
                    // if this was ever implemented, respective server handler needs to be updated accordingly
                    throw new IllegalStateException(
                            "Registering default SecurityCheck that requires secured method arguments is not supported");
                }
                return List.of(new SecurityCheckWithMethodArgsHandler(info.check, info.invokedMethodDesc));
            }

            return List.of();
        }

        private boolean requiresMethodArguments(ServerResourceMethod serverResourceMethod) {
            return getSecurityCheckInfo(serverResourceMethod).check.requiresMethodArguments();
        }

        private SecurityCheckInfo getSecurityCheckInfo(ServerResourceMethod serverResourceMethod) {
            if (securityCheckInfo == null) {
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

                securityCheckInfo = new SecurityCheckInfo(check, isDefaultJaxRsSecCheck, desc.invokedMethodDesc());
            }

            return securityCheckInfo;
        }

        private record SecurityCheckInfo(SecurityCheck check, boolean isDefaultJaxRsSecCheck,
                MethodDescription invokedMethodDesc) {

        }
    }
}
