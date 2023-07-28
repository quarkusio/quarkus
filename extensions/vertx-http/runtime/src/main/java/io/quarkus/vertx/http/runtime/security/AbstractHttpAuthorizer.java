package io.quarkus.vertx.http.runtime.security;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
abstract class AbstractHttpAuthorizer {

    private static final Logger log = Logger.getLogger(AbstractHttpAuthorizer.class);

    private final HttpAuthenticator httpAuthenticator;
    private final IdentityProviderManager identityProviderManager;
    private final AuthorizationController controller;
    private final List<HttpSecurityPolicy> policies;
    private final BlockingSecurityExecutor blockingExecutor;

    AbstractHttpAuthorizer(HttpAuthenticator httpAuthenticator, IdentityProviderManager identityProviderManager,
            AuthorizationController controller, List<HttpSecurityPolicy> policies,
            BlockingSecurityExecutor blockingExecutor) {
        this.httpAuthenticator = httpAuthenticator;
        this.identityProviderManager = identityProviderManager;
        this.controller = controller;
        this.policies = policies;
        this.blockingExecutor = blockingExecutor;
    }

    /**
     * context that allows for running blocking tasks
     */
    private final HttpSecurityPolicy.AuthorizationRequestContext CONTEXT = new HttpSecurityPolicy.AuthorizationRequestContext() {
        @Override
        public Uni<HttpSecurityPolicy.CheckResult> runBlocking(RoutingContext context, Uni<SecurityIdentity> identityUni,
                BiFunction<RoutingContext, SecurityIdentity, HttpSecurityPolicy.CheckResult> function) {
            return identityUni
                    .flatMap(new Function<SecurityIdentity, Uni<? extends HttpSecurityPolicy.CheckResult>>() {
                        @Override
                        public Uni<? extends HttpSecurityPolicy.CheckResult> apply(SecurityIdentity identity) {
                            return blockingExecutor.executeBlocking(new Supplier<HttpSecurityPolicy.CheckResult>() {
                                @Override
                                public HttpSecurityPolicy.CheckResult get() {
                                    return function.apply(context, identity);
                                }
                            });
                        }
                    });
        }
    };

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
                    routingContext.setUser(new QuarkusHttpUser(augmentedIdentity));
                    routingContext.put(QuarkusHttpUser.DEFERRED_IDENTITY_KEY, Uni.createFrom().item(augmentedIdentity));
                }
            }
            routingContext.next();
            return;
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        res.checkPermission(routingContext, identity, CONTEXT)
                .subscribe().with(new Consumer<HttpSecurityPolicy.CheckResult>() {
                    @Override
                    public void accept(HttpSecurityPolicy.CheckResult checkResult) {
                        if (!checkResult.isPermitted()) {
                            doDeny(identity, routingContext);
                        } else {
                            if (checkResult.getAugmentedIdentity() != null) {
                                doPermissionCheck(routingContext, Uni.createFrom().item(checkResult.getAugmentedIdentity()),
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
                        } else if (!(throwable instanceof AuthenticationFailedException)) {
                            //don't log auth failure
                            log.error("Exception occurred during authorization", throwable);
                        }
                    }
                });
    }

    private void doDeny(Uni<SecurityIdentity> identity, RoutingContext routingContext) {
        identity.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {

            }

            @Override
            public void onItem(SecurityIdentity identity) {
                //if we were denied we send a challenge if we are not authenticated, otherwise we send a 403
                if (identity.isAnonymous()) {
                    httpAuthenticator.sendChallenge(routingContext).subscribe().withSubscriber(new UniSubscriber<Boolean>() {
                        @Override
                        public void onSubscribe(UniSubscription subscription) {

                        }

                        @Override
                        public void onItem(Boolean item) {
                            if (!routingContext.response().ended()) {
                                routingContext.response().end();
                            }
                        }

                        @Override
                        public void onFailure(Throwable failure) {
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
                    routingContext.fail(new ForbiddenException());
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                routingContext.fail(failure);
            }
        });

    }
}
