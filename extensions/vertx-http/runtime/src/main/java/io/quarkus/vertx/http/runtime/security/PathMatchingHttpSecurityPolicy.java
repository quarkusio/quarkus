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

import javax.inject.Singleton;

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

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {
        List<HttpSecurityPolicy> permissionCheckers = findPermissionCheckers(routingContext.request());
        return doPermissionCheck(routingContext, identity, 0, permissionCheckers, requestContext);
    }

    private Uni<CheckResult> doPermissionCheck(RoutingContext routingContext,
            SecurityIdentity identity, int index,
            List<HttpSecurityPolicy> permissionCheckers, AuthorizationRequestContext requestContext) {
        if (index == permissionCheckers.size()) {
            return Uni.createFrom().item(new CheckResult(true, identity));
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        return res.checkPermission(routingContext, identity, requestContext)
                .on().item().produceUni(new Function<CheckResult, Uni<? extends CheckResult>>() {
                    @Override
                    public Uni<? extends CheckResult> apply(CheckResult checkResult) {
                        if (!checkResult.isPermitted()) {
                            return Uni.createFrom().item(CheckResult.DENY);
                        } else {
                            SecurityIdentity newIdentity = checkResult.getAugmentedIdentity() != null
                                    ? checkResult.getAugmentedIdentity()
                                    : identity;
                            //attempt to run the next checker
                            return doPermissionCheck(routingContext, newIdentity, index + 1, permissionCheckers,
                                    requestContext);
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

            for (String path : entry.getValue().paths.orElse(Collections.emptyList())) {
                if (tempMap.containsKey(path)) {
                    HttpMatcher m = new HttpMatcher(new HashSet<>(entry.getValue().methods.orElse(Collections.emptyList())),
                            checker);
                    tempMap.get(path).add(m);
                } else {
                    HttpMatcher m = new HttpMatcher(new HashSet<>(entry.getValue().methods.orElse(Collections.emptyList())),
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
