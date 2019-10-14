package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
@Singleton
public class HttpAuthorizer {

    @Inject
    HttpAuthenticator httpAuthenticator;

    @Inject
    IdentityProviderManager identityProviderManager;

    private final PathMatcher<List<HttpMatcher>> pathMatcher = new PathMatcher<>();

    public void checkPermission(RoutingContext routingContext) {
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

        List<HttpSecurityPolicy> permissionCheckers = findPermissionCheckers(routingContext.request());
        doPermissionCheck(routingContext, securityIdentity, 0, permissionCheckers);
    }

    private void doPermissionCheck(RoutingContext routingContext, SecurityIdentity securityIdentity, int index,
            List<HttpSecurityPolicy> permissionCheckers) {
        if (index == permissionCheckers.size()) {
            //we passed, nothing rejected it
            routingContext.next();
            return;
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        res.checkPermission(routingContext.request(), securityIdentity)
                .handle(new BiFunction<HttpSecurityPolicy.CheckResult, Throwable, Object>() {
                    @Override
                    public Object apply(HttpSecurityPolicy.CheckResult checkResult, Throwable throwable) {
                        if (throwable != null) {
                            routingContext.fail(throwable);
                        } else {
                            if (checkResult == HttpSecurityPolicy.CheckResult.DENY) {
                                doDeny(securityIdentity, routingContext);
                            } else {
                                //attempt to run the next checker
                                doPermissionCheck(routingContext, securityIdentity, index + 1, permissionCheckers);
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

    void init(HttpBuildTimeConfig config, Map<String, Supplier<HttpSecurityPolicy>> supplierMap) {
        Map<String, HttpSecurityPolicy> permissionCheckers = new HashMap<>();
        for (Map.Entry<String, Supplier<HttpSecurityPolicy>> i : supplierMap.entrySet()) {
            permissionCheckers.put(i.getKey(), i.getValue().get());
        }

        Map<String, List<HttpMatcher>> tempMap = new HashMap<>();
        for (Map.Entry<String, PolicyMappingConfig> entry : config.auth.permissions.entrySet()) {
            HttpSecurityPolicy checker = permissionCheckers.get(entry.getValue().policy);
            if (checker == null) {
                throw new RuntimeException("Unable to find HTTP security policy " + entry.getValue().policy);
            }

            for (String path : entry.getValue().paths) {
                if (tempMap.containsKey(path)) {
                    HttpMatcher m = new HttpMatcher(new HashSet<>(entry.getValue().methods), checker);
                    tempMap.get(path).add(m);
                } else {
                    HttpMatcher m = new HttpMatcher(new HashSet<>(entry.getValue().methods), checker);
                    List<HttpMatcher> perms = new ArrayList<>();
                    tempMap.put(path, perms);
                    perms.add(m);
                    if (path.endsWith("*")) {
                        pathMatcher.addPrefixPath(path.substring(0, path.length() - 1), perms);
                    } else {
                        pathMatcher.addExactPath(path, perms);
                    }
                }
            }
        }
    }

    public List<HttpSecurityPolicy> findPermissionCheckers(HttpServerRequest request) {
        PathMatcher.PathMatch<List<HttpMatcher>> toCheck = pathMatcher.match(request.path());
        if (toCheck.getValue() == null || toCheck.getValue().isEmpty()) {
            return Collections.emptyList();
        }
        List<HttpSecurityPolicy> methodMatch = new ArrayList<>();
        List<HttpSecurityPolicy> noMethod = new ArrayList<>();
        for (HttpMatcher i : toCheck.getValue()) {
            if (i.methods == null || i.methods.isEmpty()) {
                noMethod.add(i.checker);
            } else if (i.methods.contains(request.method().toString())) {
                methodMatch.add(i.checker);
            }
        }
        if (!methodMatch.isEmpty()) {
            return methodMatch;
        } else if (!noMethod.isEmpty()) {
            return noMethod;
        } else {
            //we deny if we did not match due to method filtering
            return Collections.singletonList(DenySecurityPolicy.INSTANCE);
        }

    }

    static class HttpMatcher {

        final Set<String> methods;
        final HttpSecurityPolicy checker;

        HttpMatcher(Set<String> methods, HttpSecurityPolicy checker) {
            this.methods = methods;
            this.checker = checker;
        }
    }
}