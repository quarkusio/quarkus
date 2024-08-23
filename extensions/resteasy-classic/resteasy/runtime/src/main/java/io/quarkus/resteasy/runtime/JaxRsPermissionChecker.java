package io.quarkus.resteasy.runtime;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;
import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.JAXRS;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
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
import io.vertx.ext.web.RoutingContext;

/**
 * Checks HTTP permissions specific for Jakarta REST.
 *
 * @see io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo#JAXRS
 */
@ApplicationScoped
public class JaxRsPermissionChecker {
    private final AbstractPathMatchingHttpSecurityPolicy jaxRsPathMatchingPolicy;
    private final HttpSecurityPolicy.AuthorizationRequestContext authorizationRequestContext;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> eventHelper;

    @Inject
    RoutingContext routingContext;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    JaxRsPermissionChecker(HttpConfiguration httpConfig, Instance<HttpSecurityPolicy> installedPolicies,
            HttpBuildTimeConfig httpBuildTimeConfig, BlockingSecurityExecutor blockingSecurityExecutor, BeanManager beanManager,
            Event<AuthorizationFailureEvent> authZFailureEvent, Event<AuthorizationSuccessEvent> authZSuccessEvent,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled) {
        var jaxRsPathMatchingPolicy = new AbstractPathMatchingHttpSecurityPolicy(httpConfig.auth.permissions,
                httpConfig.auth.rolePolicy, httpBuildTimeConfig.rootPath, installedPolicies, JAXRS);
        if (jaxRsPathMatchingPolicy.hasNoPermissions()) {
            this.jaxRsPathMatchingPolicy = null;
            this.authorizationRequestContext = null;
        } else {
            this.jaxRsPathMatchingPolicy = jaxRsPathMatchingPolicy;
            this.authorizationRequestContext = new DefaultAuthorizationRequestContext(blockingSecurityExecutor);
        }
        this.eventHelper = new SecurityEventHelper<>(authZSuccessEvent, authZFailureEvent, AUTHORIZATION_SUCCESS,
                AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
    }

    boolean shouldRunPermissionChecks() {
        return jaxRsPathMatchingPolicy != null;
    }

    void applyPermissionChecks() {
        HttpSecurityPolicy.CheckResult checkResult = jaxRsPathMatchingPolicy
                .checkPermission(routingContext, identityAssociation.getDeferredIdentity(), authorizationRequestContext)
                .await().indefinitely();
        final SecurityIdentity newIdentity;
        if (checkResult.getAugmentedIdentity() == null) {
            if (checkResult.isPermitted()) {
                // do not require authentication when permission checks didn't require it
                newIdentity = null;
            } else {
                newIdentity = identityAssociation.getIdentity();
            }
        } else if (checkResult.getAugmentedIdentity() != identityAssociation.getIdentity()) {
            newIdentity = checkResult.getAugmentedIdentity();
            QuarkusHttpUser.setIdentity(newIdentity, routingContext);
            identityAssociation.setIdentity(newIdentity);
        } else {
            newIdentity = checkResult.getAugmentedIdentity();
        }

        if (checkResult.isPermitted()) {
            if (eventHelper.fireEventOnSuccess()) {
                eventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(newIdentity,
                        AbstractPathMatchingHttpSecurityPolicy.class.getName(),
                        Map.of(RoutingContext.class.getName(), routingContext)));
            }
            return;
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

    SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> getEventHelper() {
        return eventHelper;
    }
}
