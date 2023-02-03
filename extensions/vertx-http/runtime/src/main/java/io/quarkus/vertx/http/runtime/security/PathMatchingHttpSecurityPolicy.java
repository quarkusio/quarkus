package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * A security policy that allows for matching of other security policies based on paths.
 *
 * This is used for the default path/method based RBAC.
 */
@Singleton
public class PathMatchingHttpSecurityPolicy implements HttpSecurityPolicy {

    private final PathMatcher<List<HttpMatcher>> pathMatcher = new PathMatcher<>();

    public String getAuthMechanismName(RoutingContext routingContext) {
        PathMatcher.PathMatch<List<HttpMatcher>> toCheck = pathMatcher.match(routingContext.request().path());
        if (toCheck.getValue() == null || toCheck.getValue().isEmpty()) {
            return null;
        }
        for (HttpMatcher i : toCheck.getValue()) {
            if (i.authMechanism != null) {
                return i.authMechanism;
            }
        }
        return null;
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        List<HttpSecurityPolicy> permissionCheckers = findPermissionCheckers(routingContext.request());
        return doPermissionCheck(routingContext, identity, 0, null, permissionCheckers, requestContext);
    }

    private Uni<CheckResult> doPermissionCheck(RoutingContext routingContext,
            Uni<SecurityIdentity> identity, int index, SecurityIdentity augmentedIdentity,
            List<HttpSecurityPolicy> permissionCheckers, AuthorizationRequestContext requestContext) {
        if (index == permissionCheckers.size()) {
            return Uni.createFrom().item(new CheckResult(true, augmentedIdentity));
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        return res.checkPermission(routingContext, identity, requestContext)
                .flatMap(new Function<CheckResult, Uni<? extends CheckResult>>() {
                    @Override
                    public Uni<? extends CheckResult> apply(CheckResult checkResult) {
                        if (!checkResult.isPermitted()) {
                            return Uni.createFrom().item(CheckResult.DENY);
                        } else {
                            if (checkResult.getAugmentedIdentity() != null) {

                                //attempt to run the next checker
                                return doPermissionCheck(routingContext,
                                        Uni.createFrom().item(checkResult.getAugmentedIdentity()), index + 1,
                                        checkResult.getAugmentedIdentity(),
                                        permissionCheckers,
                                        requestContext);
                            } else {
                                //attempt to run the next checker
                                return doPermissionCheck(routingContext, identity, index + 1, augmentedIdentity,
                                        permissionCheckers,
                                        requestContext);
                            }
                        }
                    }
                });
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

            if (entry.getValue().enabled.orElse(Boolean.TRUE)) {
                for (String path : entry.getValue().paths.orElse(Collections.emptyList())) {
                    path = path.trim();
                    if (!path.startsWith("/")) {
                        path = config.rootPath + path;
                    }
                    if (tempMap.containsKey(path)) {
                        HttpMatcher m = new HttpMatcher(entry.getValue().authMechanism.orElse(null),
                                new HashSet<>(entry.getValue().methods.orElse(Collections.emptyList())),
                                checker);
                        tempMap.get(path).add(m);
                    } else {
                        HttpMatcher m = new HttpMatcher(entry.getValue().authMechanism.orElse(null),
                                new HashSet<>(entry.getValue().methods.orElse(Collections.emptyList())),
                                checker);
                        List<HttpMatcher> perms = new ArrayList<>();
                        tempMap.put(path, perms);
                        perms.add(m);
                        if (path.endsWith("/*")) {
                            String stripped = path.substring(0, path.length() - 2);
                            pathMatcher.addPrefixPath(stripped.isEmpty() ? "/" : stripped, perms);
                        } else if (path.endsWith("*")) {
                            pathMatcher.addPrefixPath(path.substring(0, path.length() - 1), perms);
                        } else {
                            pathMatcher.addExactPath(path, perms);
                        }
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

        final String authMechanism;
        final Set<String> methods;
        final HttpSecurityPolicy checker;

        HttpMatcher(String authMechanism, Set<String> methods, HttpSecurityPolicy checker) {
            this.methods = methods;
            this.checker = checker;
            this.authMechanism = authMechanism;
        }
    }
}
