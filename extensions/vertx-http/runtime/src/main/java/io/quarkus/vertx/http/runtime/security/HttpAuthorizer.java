package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
@Singleton
public class HttpAuthorizer {

    private static final Logger log = Logger.getLogger(HttpAuthorizer.class);

    @Inject
    HttpAuthenticator httpAuthenticator;

    @Inject
    IdentityProviderManager identityProviderManager;

    @Inject
    AuthorizationController controller;

    final List<HttpSecurityPolicy> policies;

    @Inject
    HttpAuthorizer(Instance<HttpSecurityPolicy> installedPolicies) {
        policies = new ArrayList<>();
        for (HttpSecurityPolicy i : installedPolicies) {
            policies.add(i);
        }
    }

    /**
     * context that allows for running blocking tasks
     */
    private static final HttpSecurityPolicy.AuthorizationRequestContext CONTEXT = new HttpSecurityPolicy.AuthorizationRequestContext() {
        @Override
        public Uni<HttpSecurityPolicy.CheckResult> runBlocking(RoutingContext context, Uni<SecurityIdentity> identity,
                BiFunction<RoutingContext, SecurityIdentity, HttpSecurityPolicy.CheckResult> function) {
            if (BlockingOperationControl.isBlockingAllowed()) {
                try {
                    HttpSecurityPolicy.CheckResult res = function.apply(context, identity.await().indefinitely());
                    return Uni.createFrom().item(res);
                } catch (Throwable t) {
                    return Uni.createFrom().failure(t);
                }
            }
            try {
                return Uni.createFrom().emitter(new Consumer<UniEmitter<? super HttpSecurityPolicy.CheckResult>>() {
                    @Override
                    public void accept(UniEmitter<? super HttpSecurityPolicy.CheckResult> uniEmitter) {

                        ExecutorRecorder.getCurrent().execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    HttpSecurityPolicy.CheckResult val = function.apply(context,
                                            identity.await().indefinitely());
                                    uniEmitter.complete(val);
                                } catch (Throwable t) {
                                    uniEmitter.fail(t);
                                }
                            }
                        });
                    }
                });
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
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
                        if (!routingContext.response().ended()) {
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
                            routingContext.response().end();
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            routingContext.fail(failure);
                        }
                    });
                } else {
                    routingContext.fail(403);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                routingContext.fail(failure);
            }
        });

    }
}
