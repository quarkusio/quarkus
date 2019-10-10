package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
@ApplicationScoped
public class HttpAuthorizer {

    final List<HttpPermissionChecker> permissionCheckers;

    @Inject
    HttpAuthenticator httpAuthenticator;

    @Inject
    IdentityProviderManager identityProviderManager;

    volatile boolean defaultDeny;

    boolean isDefaultDeny() {
        return defaultDeny;
    }

    void setDefaultDeny(boolean defaultDeny) {
        this.defaultDeny = defaultDeny;
    }

    public HttpAuthorizer() {
        //for proxy
        permissionCheckers = null;
    }

    @Inject
    public HttpAuthorizer(Instance<HttpPermissionChecker> permissionCheckers) {
        this.permissionCheckers = new ArrayList<>();
        for (HttpPermissionChecker i : permissionCheckers) {
            this.permissionCheckers.add(i);
        }
        this.permissionCheckers.sort(new Comparator<HttpPermissionChecker>() {
            @Override
            public int compare(HttpPermissionChecker o1, HttpPermissionChecker o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority());
            }
        });
    }

    public void checkPermission(RoutingContext routingContext) {
        if (permissionCheckers.isEmpty()) {
            routingContext.next();
            return;
        }
        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
        if (user != null) {
            //we have a user, check their permissions
            doPermissionCheck(routingContext, user.getSecurityIdentity());
        } else {
            //otherwise check the anonymous identity
            identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE)
                    .handle(new BiFunction<SecurityIdentity, Throwable, Object>() {
                        @Override
                        public Object apply(SecurityIdentity identity, Throwable throwable) {
                            if (throwable != null) {
                                routingContext.fail(throwable);
                            } else {
                                doPermissionCheck(routingContext, identity);
                            }
                            return null;
                        }
                    });
        }
    }

    private void doPermissionCheck(RoutingContext routingContext, SecurityIdentity securityIdentity) {
        doPermissionCheck(routingContext, securityIdentity, 0);
    }

    private void doPermissionCheck(RoutingContext routingContext, SecurityIdentity securityIdentity, int index) {
        if (index == permissionCheckers.size()) {
            //we passed, nothing rejected it
            if (defaultDeny) {
                doDeny(securityIdentity, routingContext);
            } else {
                routingContext.next();
            }
            return;
        }
        //get the current checker
        HttpPermissionChecker res = permissionCheckers.get(index);
        res.checkPermission(routingContext.request(), securityIdentity)
                .handle(new BiFunction<HttpPermissionChecker.CheckResult, Throwable, Object>() {
                    @Override
                    public Object apply(HttpPermissionChecker.CheckResult checkResult, Throwable throwable) {
                        if (throwable != null) {
                            routingContext.fail(throwable);
                        } else {
                            if (checkResult == HttpPermissionChecker.CheckResult.DENY) {
                                doDeny(securityIdentity, routingContext);
                            } else if (checkResult == HttpPermissionChecker.CheckResult.PERMIT) {
                                //we are permitted, just move to the next handler
                                routingContext.next();
                            } else {
                                //attempt to run the next checker
                                doPermissionCheck(routingContext, securityIdentity, index + 1);
                            }
                        }
                        return null;
                    }
                });
    }

    private void doDeny(SecurityIdentity securityIdentity, RoutingContext routingContext) {
        //if we were denied we send a challenge if we are not authenticated, otherwise we send a 403
        if (securityIdentity.isAnonymous()) {
            httpAuthenticator.sendChallenge(routingContext, new Runnable() {
                @Override
                public void run() {
                    routingContext.response().end();
                }
            });
        } else {
            routingContext.fail(403);
        }
    }

}
