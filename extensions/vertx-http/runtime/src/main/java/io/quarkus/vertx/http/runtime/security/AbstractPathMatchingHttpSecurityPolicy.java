package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;

import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.AuthorizationRequestContext;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.CheckResult;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A security policy that allows for matching of other security policies based on paths.
 * <p>
 * This is used for the default path/method based RBAC.
 */
public class AbstractPathMatchingHttpSecurityPolicy {

    private final PathMatcher<List<HttpMatcher>> pathMatcher = new PathMatcher<>();

    AbstractPathMatchingHttpSecurityPolicy(Map<String, PolicyMappingConfig> permissions,
            Map<String, PolicyConfig> rolePolicy, String rootPath, Map<String, HttpSecurityPolicy> namedBuildTimePolicies) {
        init(permissions, toNamedHttpSecPolicies(rolePolicy, namedBuildTimePolicies), rootPath);
    }

    public String getAuthMechanismName(RoutingContext routingContext) {
        PathMatcher.PathMatch<List<HttpMatcher>> toCheck = pathMatcher.match(routingContext.normalizedPath());
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

    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        List<HttpSecurityPolicy> permissionCheckers = findPermissionCheckers(routingContext);
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

    private void init(Map<String, PolicyMappingConfig> permissions,
            Map<String, HttpSecurityPolicy> permissionCheckers, String rootPath) {
        Map<String, List<HttpMatcher>> tempMap = new HashMap<>();
        for (Map.Entry<String, PolicyMappingConfig> entry : permissions.entrySet()) {
            HttpSecurityPolicy checker = permissionCheckers.get(entry.getValue().policy);
            if (checker == null) {
                throw new RuntimeException("Unable to find HTTP security policy " + entry.getValue().policy);
            }

            if (entry.getValue().enabled.orElse(Boolean.TRUE)) {
                for (String path : entry.getValue().paths.orElse(Collections.emptyList())) {
                    path = path.trim();
                    if (!path.startsWith("/")) {
                        path = rootPath + path;
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

    public List<HttpSecurityPolicy> findPermissionCheckers(RoutingContext context) {
        PathMatcher.PathMatch<List<HttpMatcher>> toCheck = pathMatcher.match(context.normalizedPath());
        if (toCheck.getValue() == null || toCheck.getValue().isEmpty()) {
            return Collections.emptyList();
        }
        List<HttpSecurityPolicy> methodMatch = new ArrayList<>();
        List<HttpSecurityPolicy> noMethod = new ArrayList<>();
        for (HttpMatcher i : toCheck.getValue()) {
            if (i.methods == null || i.methods.isEmpty()) {
                noMethod.add(i.checker);
            } else if (i.methods.contains(context.request().method().toString())) {
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

    private static Map<String, HttpSecurityPolicy> toNamedHttpSecPolicies(Map<String, PolicyConfig> rolePolicies,
            Map<String, HttpSecurityPolicy> namedBuildTimePolicies) {
        Map<String, HttpSecurityPolicy> namedPolicies = new HashMap<>();
        if (!namedBuildTimePolicies.isEmpty()) {
            namedPolicies.putAll(namedBuildTimePolicies);
        }
        for (Map.Entry<String, PolicyConfig> e : rolePolicies.entrySet()) {
            PolicyConfig policyConfig = e.getValue();
            if (policyConfig.permissions.isEmpty()) {
                namedPolicies.put(e.getKey(), new RolesAllowedHttpSecurityPolicy(policyConfig.rolesAllowed));
            } else {
                final Map<String, Set<Permission>> roleToPermissions = new HashMap<>();
                for (Map.Entry<String, List<String>> roleToPermissionStr : policyConfig.permissions.entrySet()) {

                    // collect permission actions
                    // perm1:action1,perm2:action2,perm1:action3 -> perm1:action1,action3 and perm2:action2
                    Map<String, PermissionToActions> cache = new HashMap<>();
                    final String role = roleToPermissionStr.getKey();
                    for (String permissionToAction : roleToPermissionStr.getValue()) {
                        // parse permission to actions and add it to cache
                        addPermissionToAction(cache, role, permissionToAction);
                    }

                    // create permissions
                    var permissions = new HashSet<Permission>();
                    for (PermissionToActions helper : cache.values()) {
                        if (StringPermission.class.getName().equals(policyConfig.permissionClass)) {
                            permissions.add(new StringPermission(helper.permissionName, helper.actions.toArray(new String[0])));
                        } else {
                            permissions.add(customPermissionCreator(policyConfig, helper));
                        }
                    }

                    roleToPermissions.put(role, Set.copyOf(permissions));
                }
                namedPolicies.put(e.getKey(),
                        new RolesAllowedHttpSecurityPolicy(policyConfig.rolesAllowed, Map.copyOf(roleToPermissions)));
            }
        }
        namedPolicies.put("deny", new DenySecurityPolicy());
        namedPolicies.put("permit", new PermitSecurityPolicy());
        namedPolicies.put("authenticated", new AuthenticatedHttpSecurityPolicy());
        return namedPolicies;
    }

    private static boolean acceptsActions(String permissionClassStr) {
        var permissionClass = loadClass(permissionClassStr);
        if (permissionClass.getConstructors().length != 1) {
            throw new ConfigurationException(
                    String.format("Permission class '%s' must have exactly one constructor", permissionClass));
        }
        var constructor = permissionClass.getConstructors()[0];
        // first parameter must be permission name (String)
        if (constructor.getParameterCount() == 0 || !(constructor.getParameterTypes()[0] == String.class)) {
            throw new ConfigurationException(
                    String.format("Permission class '%s' constructor first parameter must be '%s' (permission name)",
                            permissionClass, String.class.getName()));
        }
        final boolean acceptsActions;
        if (constructor.getParameterCount() == 1) {
            acceptsActions = false;
        } else {
            if (constructor.getParameterCount() == 2) {
                if (constructor.getParameterTypes()[1] != String[].class) {
                    throw new ConfigurationException(
                            String.format("Permission class '%s' constructor second parameter must be '%s' array",
                                    permissionClass,
                                    String.class.getName()));
                }
            } else {
                throw new ConfigurationException(String.format(
                        "Permission class '%s' constructor must accept either one parameter (String permissionName), or two parameters (String permissionName, String[] actions)",
                        permissionClass));
            }
            acceptsActions = true;
        }
        return acceptsActions;
    }

    private static void addPermissionToAction(Map<String, PermissionToActions> cache, String role, String permissionToAction) {
        final String permissionName;
        final String action;
        // incoming value is either in format perm1:action1 or perm1 (with or withot action)
        if (permissionToAction.contains(PERMISSION_TO_ACTION_SEPARATOR)) {
            // perm1:action1
            var permToActions = permissionToAction.split(PERMISSION_TO_ACTION_SEPARATOR);
            if (permToActions.length != 2) {
                throw new ConfigurationException(
                        String.format("Invalid permission format '%s', please use exactly one permission to action separator",
                                permissionToAction));
            }
            permissionName = permToActions[0].trim();
            action = permToActions[1].trim();
        } else {
            // perm1
            permissionName = permissionToAction.trim();
            action = null;
        }

        if (permissionName.isEmpty()) {
            throw new ConfigurationException(
                    String.format("Invalid permission name '%s' for role '%s'", permissionToAction, role));
        }

        cache.computeIfAbsent(permissionName, new Function<String, PermissionToActions>() {
            @Override
            public PermissionToActions apply(String s) {
                return new PermissionToActions(s);
            }
        }).addAction(action);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for creating permission", e);
        }
    }

    private static Permission customPermissionCreator(PolicyConfig policyConfig, PermissionToActions helper) {
        try {
            var constructor = loadClass(policyConfig.permissionClass).getConstructors()[0];
            if (acceptsActions(policyConfig.permissionClass)) {
                return (Permission) constructor.newInstance(helper.permissionName, helper.actions.toArray(new String[0]));
            } else {
                return (Permission) constructor.newInstance(helper.permissionName);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to create Permission - class '%s', name '%s', actions '%s'",
                    policyConfig.permissionClass, helper.permissionName,
                    Arrays.toString(helper.actions.toArray(new String[0]))), e);
        }
    }

    private static final class PermissionToActions {
        private final String permissionName;
        private final Set<String> actions;

        private PermissionToActions(String permissionName) {
            this.permissionName = permissionName;
            this.actions = new HashSet<>();
        }

        private void addAction(String action) {
            if (action != null) {
                this.actions.add(action);
            }
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
