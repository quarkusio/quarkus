package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;
import static io.quarkus.vertx.http.runtime.security.QuarkusHttpUser.setIdentity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
abstract class AbstractHttpAuthorizer {

    private static final Logger log = Logger.getLogger(AbstractHttpAuthorizer.class);

    private final IdentityProviderManager identityProviderManager;
    private final AuthorizationController controller;
    private final List<HttpSecurityPolicy> policies;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper;
    private final HttpSecurityPolicy.AuthorizationRequestContext context;

    AbstractHttpAuthorizer(IdentityProviderManager identityProviderManager,
            AuthorizationController controller, List<HttpSecurityPolicy> policies, BeanManager beanManager,
            BlockingSecurityExecutor blockingExecutor, Event<AuthorizationFailureEvent> authZFailureEvent,
            Event<AuthorizationSuccessEvent> authZSuccessEvent, boolean securityEventsEnabled) {
        this.identityProviderManager = identityProviderManager;
        this.controller = controller;
        this.policies = policies;
        this.context = new HttpSecurityPolicy.DefaultAuthorizationRequestContext(blockingExecutor);
        this.securityEventHelper = new SecurityEventHelper<>(authZSuccessEvent, authZFailureEvent, AUTHORIZATION_SUCCESS,
                AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
    }

    /**
     * Checks that the request is allowed to proceed. If it is then {@link RoutingContext#next()} will
     * be invoked, if not appropriate action will be taken to either report the failure or attempt authentication.
     */
    public void checkPermission(RoutingContext routingContext) {
        if (!controller.isAuthorizationEnabled()) {
            routingContext.next();
            return;
        }
        //check their permissions
        doPermissionCheck(routingContext, QuarkusHttpUser.getSecurityIdentity(routingContext, identityProviderManager), 0, null,
                policies);
    }

    private void doPermissionCheck(RoutingContext routingContext,
            Uni<SecurityIdentity> identity, int index,
            SecurityIdentity augmentedIdentity,
            List<HttpSecurityPolicy> permissionCheckers) {
        if (index == permissionCheckers.size()) {
            QuarkusHttpUser currentUser = (QuarkusHttpUser) routingContext.user();
            if (augmentedIdentity != null) {
                if (!augmentedIdentity.isAnonymous()
                        && (currentUser == null || currentUser.getSecurityIdentity() != augmentedIdentity)) {
                    setIdentity(augmentedIdentity, routingContext);
                }
                if (securityEventHelper.fireEventOnSuccess()) {
                    securityEventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(augmentedIdentity,
                            Map.of(RoutingContext.class.getName(), routingContext)));
                }
            } else if (securityEventHelper.fireEventOnSuccess()
                    && permissionCheckPerformed(permissionCheckers, routingContext, index)) {
                securityEventHelper.fireSuccessEvent(
                        new AuthorizationSuccessEvent(currentUser == null ? null : currentUser.getSecurityIdentity(),
                                Map.of(RoutingContext.class.getName(), routingContext)));
            }
            routingContext.next();
            return;
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        res.checkPermission(routingContext, identity, context)
                .subscribe().with(new Consumer<HttpSecurityPolicy.CheckResult>() {
                    @Override
                    public void accept(HttpSecurityPolicy.CheckResult checkResult) {
                        if (!checkResult.isPermitted()) {
                            doDeny(identity, routingContext, res, checkResult.getAugmentedIdentity());
                        } else {
                            if (checkResult.getAugmentedIdentity() != null) {
                                doPermissionCheck(routingContext, checkResult.getAugmentedIdentityAsUni(),
                                        index + 1, checkResult.getAugmentedIdentity(), permissionCheckers);
                            } else {
                                //attempt to run the next checker
                                doPermissionCheck(routingContext, identity, index + 1, augmentedIdentity, permissionCheckers);
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        // we don't fail event if it's already failed with same exception as we don't want to process
                        // the exception twice;at this point, the exception could be failed by the default auth failure handler
                        if (!routingContext.response().ended() && !throwable.equals(routingContext.failure())) {
                            routingContext.fail(throwable);
                        } else if (throwable instanceof AuthenticationFailedException) {
                            log.debug("Authentication challenge is required");
                        } else if (throwable instanceof AuthenticationRedirectException) {
                            log.debugf("Completing authentication with a redirect to %s",
                                    ((AuthenticationRedirectException) throwable).getRedirectUri());
                        } else {
                            log.error("Exception occurred during authorization", throwable);
                        }

                    }
                });
    }

    private void doDeny(SecurityIdentity identity, RoutingContext routingContext, HttpSecurityPolicy policy) {
        //if we were denied we send a challenge if we are not authenticated, otherwise we send a 403
        if (identity.isAnonymous()) {
            HttpAuthenticator httpAuthenticator = routingContext.get(HttpAuthenticator.class.getName());
            httpAuthenticator.sendChallenge(routingContext).subscribe().withSubscriber(new UniSubscriber<Boolean>() {
                @Override
                public void onSubscribe(UniSubscription subscription) {

                }

                @Override
                public void onItem(Boolean item) {
                    if (!routingContext.response().ended()) {
                        routingContext.response().end();
                    }
                    fireAuthZFailureEvent(routingContext, policy, null, identity);
                }

                @Override
                public void onFailure(Throwable failure) {
                    fireAuthZFailureEvent(routingContext, policy, failure, identity);
                    if (!routingContext.response().ended()) {
                        routingContext.fail(failure);
                    } else if (!(failure instanceof IOException)) {
                        log.error("Failed to send challenge", failure);
                    } else {
                        log.debug("Failed to send challenge", failure);
                    }
                }
            });
        } else {
            ForbiddenException forbiddenException = new ForbiddenException();
            fireAuthZFailureEvent(routingContext, policy, forbiddenException, identity);
            routingContext.fail(new ForbiddenException());
        }
    }

    private void doDeny(Uni<SecurityIdentity> identity, RoutingContext routingContext, HttpSecurityPolicy policy,
            SecurityIdentity augmentedIdentity) {
        if (augmentedIdentity == null) {
            identity.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                @Override
                public void onSubscribe(UniSubscription subscription) {

                }

                @Override
                public void onItem(SecurityIdentity identity) {
                    doDeny(identity, routingContext, policy);
                }

                @Override
                public void onFailure(Throwable failure) {
                    fireAuthZFailureEvent(routingContext, policy, failure, null);
                    routingContext.fail(failure);
                }
            });
        } else {
            doDeny(augmentedIdentity, routingContext, policy);
        }
    }

    private void fireAuthZFailureEvent(RoutingContext routingContext, HttpSecurityPolicy policy, Throwable failure,
            SecurityIdentity identity) {
        if (securityEventHelper.fireEventOnFailure()) {
            final String context = policy != null ? policy.getClass().getName() : null;
            final AuthorizationFailureEvent event = new AuthorizationFailureEvent(identity, failure, context,
                    Map.of(RoutingContext.class.getName(), routingContext));
            securityEventHelper.fireFailureEvent(event);
        }
    }

    private static boolean permissionCheckPerformed(List<HttpSecurityPolicy> permissionCheckers,
            RoutingContext routingContext, int index) {
        // the path matching policy is not permission check itself, it selects policy based on
        // configured HTTP permissions, but if there are no path matching HTTP permissions, there is no check
        if (index == 1 && permissionCheckers.get(0) instanceof AbstractPathMatchingHttpSecurityPolicy) {
            return AbstractPathMatchingHttpSecurityPolicy.policyApplied(routingContext);
        }
        return index > 0;
    }
}
