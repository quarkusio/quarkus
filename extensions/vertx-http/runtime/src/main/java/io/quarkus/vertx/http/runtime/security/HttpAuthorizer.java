package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

    private final PathMatcher<List<HttpMatcher>> pathMatcher = new PathMatcher<>();
    @Inject
    HttpAuthenticator httpAuthenticator;
    @Inject
    IdentityProviderManager identityProviderManager;

    public CompletionStage<SecurityIdentity> checkPermission(RoutingContext routingContext) {
        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
        if (user == null) {
            //check the anonymous identity
            return attemptAnonymousAuthentication(routingContext);
        }
        //we have a user, check their permissions
        return doPermissionCheck(routingContext, user.getSecurityIdentity());
    }

    protected CompletableFuture<SecurityIdentity> attemptAnonymousAuthentication(RoutingContext routingContext) {
        CompletableFuture<SecurityIdentity> latch = new CompletableFuture<>();
        identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE)
                .handle(new BiFunction<SecurityIdentity, Throwable, Object>() {
                    @Override
                    public Object apply(SecurityIdentity identity, Throwable throwable) {
                        if (throwable != null) {
                            latch.completeExceptionally(throwable);
                        } else {
                            doPermissionCheck(routingContext, identity).handle(
                                    new BiFunction<SecurityIdentity, Throwable, SecurityIdentity>() {
                                        @Override
                                        public SecurityIdentity apply(SecurityIdentity identity,
                                                Throwable throwable) {
                                            if (throwable != null) {
                                                latch.completeExceptionally(throwable);
                                                return null;
                                            }
                                            latch.complete(identity);
                                            return identity;
                                        }
                                    });
                        }
                        return null;
                    }
                });
        return latch;
    }

    private CompletionStage<SecurityIdentity> doPermissionCheck(RoutingContext routingContext,
            SecurityIdentity identity) {
        CompletableFuture<SecurityIdentity> latch = new CompletableFuture<>();
        List<HttpSecurityPolicy> permissionCheckers = findPermissionCheckers(routingContext.request());
        doPermissionCheck(routingContext, latch, identity, 0, permissionCheckers);
        return latch;
    }

    private void doPermissionCheck(RoutingContext routingContext, CompletableFuture<SecurityIdentity> latch,
            SecurityIdentity identity, int index,
            List<HttpSecurityPolicy> permissionCheckers) {
        if (index == permissionCheckers.size()) {
            latch.complete(identity);
            return;
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        res.checkPermission(routingContext.request(), identity)
                .handle(new BiFunction<HttpSecurityPolicy.CheckResult, Throwable, Object>() {
                    @Override
                    public Object apply(HttpSecurityPolicy.CheckResult checkResult, Throwable throwable) {
                        if (throwable != null) {
                            latch.completeExceptionally(throwable);
                        } else {
                            if (checkResult == HttpSecurityPolicy.CheckResult.DENY) {
                                doDeny(identity, routingContext, latch);
                            } else {
                                //attempt to run the next checker
                                doPermissionCheck(routingContext, latch, identity, index + 1, permissionCheckers);
                            }
                        }
                        return null;
                    }
                });
    }

    private void doDeny(SecurityIdentity identity, RoutingContext routingContext,
            CompletableFuture<SecurityIdentity> latch) {
        //if we were denied we send a challenge if we are not authenticated, otherwise we send a 403
        if (identity.isAnonymous()) {
            httpAuthenticator.sendChallenge(routingContext, new Runnable() {
                @Override
                public void run() {
                    routingContext.response().end();
                }
            });
        } else {
            routingContext.fail(403);
        }
        latch.complete(null);
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
