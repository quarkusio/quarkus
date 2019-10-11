package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.PermissionSetConfig;
import io.vertx.core.http.HttpServerRequest;

/**
 * permission checker that handles permissions defined in application.properties
 */
@ApplicationScoped
public class DefaultHttpPermissionChecker implements HttpPermissionChecker {

    private final PathMatcher<List<PermissionSetConfig>> pathMatcher = new PathMatcher<>();

    void init(HttpBuildTimeConfig config) {
        Map<String, List<PermissionSetConfig>> tempMap = new HashMap<>();
        for (Map.Entry<String, PermissionSetConfig> entry : config.auth.permissions.entrySet()) {
            if (entry.getValue().permitAll && entry.getValue().denyAll) {
                throw new IllegalStateException("Cannot set permit-all and deny-all on HTTP permission " + entry.getKey());
            } else if (!entry.getValue().rolesAllowed.isEmpty()) {
                if (entry.getValue().permitAll || entry.getValue().denyAll) {
                    throw new IllegalStateException(
                            "Cannot set permit-all and deny-all on HTTP permission when roles-allowed has also been set on HTTP permission"
                                    + entry.getKey());
                }
            } else if (!entry.getValue().denyAll && !entry.getValue().permitAll) {
                throw new IllegalStateException(
                        "No security constraints set on HTTP permission "
                                + entry.getKey());
            }
            for (String path : entry.getValue().paths) {
                if (tempMap.containsKey(path)) {
                    tempMap.get(path).add(entry.getValue());
                } else {
                    List<PermissionSetConfig> perms = new ArrayList<>();
                    tempMap.put(path, perms);
                    perms.add(entry.getValue());
                    if (path.endsWith("*")) {
                        pathMatcher.addPrefixPath(path.substring(0, path.length() - 1), perms);
                    } else {
                        pathMatcher.addExactPath(path, perms);
                    }
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public CompletionStage<CheckResult> checkPermission(HttpServerRequest request, SecurityIdentity identity) {
        PathMatcher.PathMatch<List<PermissionSetConfig>> toCheck = pathMatcher.match(request.path());
        if (toCheck.getValue() == null) {
            return CompletableFuture.completedFuture(CheckResult.IGNORE);
        }
        List<PermissionSetConfig> methodMatch = new ArrayList<>();
        List<PermissionSetConfig> noMethod = new ArrayList<>();
        for (PermissionSetConfig i : toCheck.getValue()) {
            if (i.methods == null || i.methods.isEmpty()) {
                noMethod.add(i);
            } else if (i.methods.contains(request.method().toString())) {
                methodMatch.add(i);
            }
        }
        if (!methodMatch.isEmpty()) {
            return handleMatch(methodMatch, identity);
        } else if (!noMethod.isEmpty()) {
            return handleMatch(noMethod, identity);
        }
        //we matched, but not on the method, so we deny all other methods
        return CompletableFuture.completedFuture(CheckResult.DENY);

    }

    private CompletionStage<CheckResult> handleMatch(List<PermissionSetConfig> toCheck, SecurityIdentity identity) {

        for (PermissionSetConfig i : toCheck) {
            if (i.permitAll) {
                continue;
            }
            if (i.denyAll) {
                return CompletableFuture.completedFuture(CheckResult.DENY);
            }
            boolean roleFound = false;
            for (String role : i.rolesAllowed) {
                if (role.equals("*") && !identity.isAnonymous()) {
                    roleFound = true;
                    break;
                }
                if (identity.getRoles().contains(role)) {
                    roleFound = true;
                    break;
                }
            }
            if (!roleFound) {
                return CompletableFuture.completedFuture(CheckResult.DENY);
            }
        }
        return CompletableFuture.completedFuture(CheckResult.PERMIT);
    }
}
