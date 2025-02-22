package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor.STANDARD_SECURITY_CHECK_INTERCEPTOR;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.JaxRsPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public final class EagerSecurityContext {

    private static volatile EagerSecurityContext instance = null;
    private final JaxRsPathMatchingHttpSecurityPolicy jaxRsPathMatchingPolicy;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> eventHelper;
    private final InjectableInstance<CurrentIdentityAssociation> identityAssociation;
    private final AuthorizationController authorizationController;
    private final boolean doNotRunPermissionSecurityCheck;
    private final boolean isProactiveAuthDisabled;

    EagerSecurityContext(Event<AuthorizationFailureEvent> authorizationFailureEvent,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled,
            Event<AuthorizationSuccessEvent> authorizationSuccessEvent, BeanManager beanManager,
            InjectableInstance<CurrentIdentityAssociation> identityAssociation, AuthorizationController authorizationController,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            JaxRsPathMatchingHttpSecurityPolicy jaxRsPathMatchingPolicy) {
        this.isProactiveAuthDisabled = !httpBuildTimeConfig.auth().proactive();
        this.identityAssociation = identityAssociation;
        this.authorizationController = authorizationController;
        this.eventHelper = new SecurityEventHelper<>(authorizationSuccessEvent, authorizationFailureEvent,
                AUTHORIZATION_SUCCESS, AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
        if (jaxRsPathMatchingPolicy.hasNoPermissions()) {
            this.jaxRsPathMatchingPolicy = null;
            this.doNotRunPermissionSecurityCheck = true;
        } else {
            this.jaxRsPathMatchingPolicy = jaxRsPathMatchingPolicy;
            this.doNotRunPermissionSecurityCheck = false;
        }
    }

    void initSingleton(@Observes StartupEvent event) {
        // intention here is to initialize this instance during app startup and make it accessible as singleton to
        // all the security ServerRestHandler instances, so that they don't need to access it via CDI programmatically
        // and write to a volatile variable during the request; the EagerSecurityHandler is created for each
        // endpoint (in case there is HTTP permission configured), so there can be a lot of them
        instance = this;
    }

    Uni<SecurityIdentity> getDeferredIdentity() {
        return Uni.createFrom().deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> get() {
                return identityAssociation.get().getDeferredIdentity();
            }
        });
    }

    Uni<SecurityIdentity> getPermissionCheck(ResteasyReactiveRequestContext requestContext, SecurityIdentity identity,
            MethodDescription invokedMethodDesc) {
        final RoutingContext routingContext = requestContext.unwrap(RoutingContext.class);
        if (routingContext == null) {
            throw new IllegalStateException(
                    "HTTP Security policy applied only on Quarkus REST cannot be run as 'RoutingContext' is null");
        }
        record SecurityCheckWithIdentity(SecurityIdentity identity, HttpSecurityPolicy.CheckResult checkResult) {
        }
        return jaxRsPathMatchingPolicy
                .checkPermission(routingContext, identity == null ? getDeferredIdentity() : Uni.createFrom().item(identity),
                        invokedMethodDesc)
                .flatMap(new Function<HttpSecurityPolicy.CheckResult, Uni<? extends SecurityCheckWithIdentity>>() {
                    @Override
                    public Uni<SecurityCheckWithIdentity> apply(HttpSecurityPolicy.CheckResult checkResult) {
                        if (identity != null) {
                            return Uni.createFrom().item(new SecurityCheckWithIdentity(identity, checkResult));
                        }
                        if (checkResult.isPermitted() && checkResult.getAugmentedIdentity() == null) {
                            return Uni.createFrom().item(new SecurityCheckWithIdentity(null, checkResult));
                        }
                        // we need to resolve identity either to compare augmented identity or to determine
                        // whether the identity is anonymous (determines thrown exception for denied access)
                        return getDeferredIdentity().map(new Function<SecurityIdentity, SecurityCheckWithIdentity>() {
                            @Override
                            public SecurityCheckWithIdentity apply(SecurityIdentity identity1) {
                                return new SecurityCheckWithIdentity(identity1, checkResult);
                            }
                        });
                    }
                })
                .map(new Function<SecurityCheckWithIdentity, SecurityIdentity>() {
                    @Override
                    public SecurityIdentity apply(SecurityCheckWithIdentity checkWithIdentity) {
                        final HttpSecurityPolicy.CheckResult checkResult = checkWithIdentity.checkResult();
                        final SecurityIdentity newIdentity;
                        if (checkResult.getAugmentedIdentity() == null) {
                            newIdentity = checkWithIdentity.identity();
                        } else if (checkResult.getAugmentedIdentity() != checkWithIdentity.identity()) {
                            newIdentity = checkResult.getAugmentedIdentity();
                            QuarkusHttpUser.setIdentity(newIdentity, routingContext);
                            identityAssociation.get().setIdentity(newIdentity);
                        } else {
                            newIdentity = checkResult.getAugmentedIdentity();
                        }

                        // access granted
                        if (checkResult.isPermitted()) {
                            if (eventHelper.fireEventOnSuccess()) {
                                eventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(newIdentity,
                                        AbstractPathMatchingHttpSecurityPolicy.class.getName(),
                                        Map.of(RoutingContext.class.getName(), routingContext)));
                            }
                            return newIdentity;
                        }

                        // access denied
                        final RuntimeException exception;
                        if (newIdentity.isAnonymous()) {
                            exception = new UnauthorizedException();
                        } else {
                            exception = new ForbiddenException();
                        }
                        if (eventHelper.fireEventOnFailure()) {
                            eventHelper.fireFailureEvent(new AuthorizationFailureEvent(newIdentity, exception,
                                    AbstractPathMatchingHttpSecurityPolicy.class.getName(),
                                    Map.of(RoutingContext.class.getName(), routingContext)));
                        }
                        throw exception;
                    }
                });
    }

    Uni<?> runSecurityCheck(SecurityCheck check, MethodDescription invokedMethodDesc,
            ResteasyReactiveRequestContext requestContext, SecurityIdentity securityIdentity) {
        preventRepeatedSecurityChecks(requestContext, invokedMethodDesc);
        var checkResult = check.nonBlockingApply(securityIdentity, invokedMethodDesc,
                requestContext.getParameters());
        if (eventHelper.fireEventOnFailure()) {
            checkResult = checkResult
                    .onFailure()
                    .invoke(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            eventHelper
                                    .fireFailureEvent(new AuthorizationFailureEvent(
                                            securityIdentity, throwable, check.getClass().getName(),
                                            createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
                        }
                    });
        }
        if (eventHelper.fireEventOnSuccess()) {
            checkResult = checkResult
                    .invoke(new Runnable() {
                        @Override
                        public void run() {
                            eventHelper.fireSuccessEvent(
                                    new AuthorizationSuccessEvent(securityIdentity,
                                            check.getClass().getName(),
                                            createEventPropsWithRoutingCtx(requestContext), invokedMethodDesc));
                        }
                    });
        }
        return checkResult;
    }

    static void preventRepeatedSecurityChecks(ResteasyReactiveRequestContext requestContext,
            MethodDescription methodDescription) {
        // propagate information that security check has been performed on this method to the SecurityHandler
        // via io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor
        requestContext.setProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR, methodDescription);
    }

    static Map<String, Object> createEventPropsWithRoutingCtx(ResteasyReactiveRequestContext requestContext) {
        final RoutingContext routingContext = requestContext.unwrap(RoutingContext.class);
        if (routingContext == null) {
            return Map.of();
        } else {
            return Map.of(RoutingContext.class.getName(), routingContext);
        }
    }

    static EagerSecurityContext getInstance() {
        if (instance == null) {
            InjectableInstance<EagerSecurityContext> contextInstance = Arc.container().select(EagerSecurityContext.class);
            if (contextInstance.isResolvable()) {
                instance = contextInstance.get();
            } else {
                // only true when Security extension is not present, in which case users can create their
                // own Jakarta REST filters that perform security and provide security identity association
                // relevant for the SecurityContextOverrideHandler that is added regardless of the Security extension
                return null;
            }
        }
        return instance;
    }

    static boolean isAuthorizationEnabled() {
        return getInstance().authorizationController.isAuthorizationEnabled();
    }

    static CurrentIdentityAssociation getCurrentIdentityAssociation() {
        var instance = getInstance();
        if (instance == null) {
            return null;
        }
        return instance.identityAssociation.get();
    }

    static SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> getEventHelper() {
        return getInstance().eventHelper;
    }

    static boolean doNotRunPermissionSecurityCheck() {
        return getInstance().doNotRunPermissionSecurityCheck;
    }

    static boolean isProactiveAuthDisabled() {
        return getInstance().isProactiveAuthDisabled;
    }
}
