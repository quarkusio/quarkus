package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;
import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.JAXRS;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.DefaultAuthorizationRequestContext;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class EagerSecurityContext {

    static EagerSecurityContext instance = null;
    private final HttpSecurityPolicy.AuthorizationRequestContext authorizationRequestContext;
    final AbstractPathMatchingHttpSecurityPolicy jaxRsPathMatchingPolicy;
    final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> eventHelper;
    final InjectableInstance<CurrentIdentityAssociation> identityAssociation;
    final AuthorizationController authorizationController;
    final boolean doNotRunPermissionSecurityCheck;
    final boolean isProactiveAuthDisabled;

    EagerSecurityContext(Event<AuthorizationFailureEvent> authorizationFailureEvent,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled,
            Event<AuthorizationSuccessEvent> authorizationSuccessEvent, BeanManager beanManager,
            InjectableInstance<CurrentIdentityAssociation> identityAssociation, AuthorizationController authorizationController,
            HttpConfiguration httpConfig, BlockingSecurityExecutor blockingExecutor,
            HttpBuildTimeConfig buildTimeConfig, Instance<HttpSecurityPolicy> installedPolicies) {
        this.isProactiveAuthDisabled = !buildTimeConfig.auth.proactive;
        this.identityAssociation = identityAssociation;
        this.authorizationController = authorizationController;
        this.eventHelper = new SecurityEventHelper<>(authorizationSuccessEvent, authorizationFailureEvent,
                AUTHORIZATION_SUCCESS, AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
        var jaxRsPathMatchingPolicy = new AbstractPathMatchingHttpSecurityPolicy(httpConfig.auth.permissions,
                httpConfig.auth.rolePolicy, buildTimeConfig.rootPath, installedPolicies, JAXRS);
        if (jaxRsPathMatchingPolicy.hasNoPermissions()) {
            this.jaxRsPathMatchingPolicy = null;
            this.authorizationRequestContext = null;
            this.doNotRunPermissionSecurityCheck = true;
        } else {
            this.jaxRsPathMatchingPolicy = jaxRsPathMatchingPolicy;
            this.authorizationRequestContext = new DefaultAuthorizationRequestContext(blockingExecutor);
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

    void destroySingleton(@Observes ShutdownEvent event) {
        instance = null;
    }

    Uni<SecurityIdentity> getDeferredIdentity() {
        return Uni.createFrom().deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> get() {
                return EagerSecurityContext.instance.identityAssociation.get().getDeferredIdentity();
            }
        });
    }

    Uni<SecurityIdentity> getPermissionCheck(ResteasyReactiveRequestContext requestContext, SecurityIdentity identity) {
        final RoutingContext routingContext = requestContext.unwrap(RoutingContext.class);
        if (routingContext == null) {
            throw new IllegalStateException(
                    "HTTP Security policy applied only on Quarkus REST cannot be run as 'RoutingContext' is null");
        }
        record SecurityCheckWithIdentity(SecurityIdentity identity, HttpSecurityPolicy.CheckResult checkResult) {
        }
        return jaxRsPathMatchingPolicy
                .checkPermission(routingContext, identity == null ? getDeferredIdentity() : Uni.createFrom().item(identity),
                        authorizationRequestContext)
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
}
