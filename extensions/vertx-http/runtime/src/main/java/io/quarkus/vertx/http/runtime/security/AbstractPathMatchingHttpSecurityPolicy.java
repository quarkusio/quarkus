package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;

import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.ClientProxy;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.AuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.AuthorizationRequestContext;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.CheckResult;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher.PathMatch;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A security policy that allows for matching of other security policies based on paths.
 * <p>
 * This is used for the default path/method based RBAC.
 */
public class AbstractPathMatchingHttpSecurityPolicy {

    private static final String PATH_MATCHING_POLICY_FOUND = AbstractPathMatchingHttpSecurityPolicy.class.getName()
            + ".POLICY_FOUND";
    private final ImmutablePathMatcher<List<HttpMatcher>> pathMatcher;
    private final List<ImmutablePathMatcher<List<HttpMatcher>>> sharedPermissionsPathMatchers;
    private final boolean hasNoPermissions;

    AbstractPathMatchingHttpSecurityPolicy(List<HttpSecurityConfiguration.HttpPermissionCarrier> httpPermissions,
            Map<String, PolicyConfig> rolePolicy, String rootPath, Instance<HttpSecurityPolicy> installedPolicies,
            PolicyMappingConfig.AppliesTo appliesTo) {
        boolean hasNoPermissions = true;
        var namedHttpSecurityPolicies = toNamedHttpSecPolicies(rolePolicy, installedPolicies);
        List<ImmutablePathMatcher<List<HttpMatcher>>> sharedPermsMatchers = new ArrayList<>();
        final var builder = ImmutablePathMatcher.<List<HttpMatcher>> builder().handlerAccumulator(List::addAll)
                .rootPath(rootPath);
        for (var httpPermission : httpPermissions) {
            if (appliesTo != httpPermission.getAppliesTo()) {
                continue;
            }
            if (hasNoPermissions) {
                hasNoPermissions = false;
            }
            if (httpPermission.isShared()) {
                final var builder1 = ImmutablePathMatcher.<List<HttpMatcher>> builder().handlerAccumulator(List::addAll)
                        .rootPath(rootPath);
                addPermissionToPathMatcher(namedHttpSecurityPolicies, httpPermission, builder1);
                sharedPermsMatchers.add(builder1.build());
            } else {
                addPermissionToPathMatcher(namedHttpSecurityPolicies, httpPermission, builder);
            }
        }
        this.hasNoPermissions = hasNoPermissions;
        this.sharedPermissionsPathMatchers = sharedPermsMatchers.isEmpty() ? null : List.copyOf(sharedPermsMatchers);
        this.pathMatcher = builder.build();
    }

    AuthenticationMechanism getAuthMechanism(RoutingContext routingContext) {
        if (sharedPermissionsPathMatchers != null) {
            for (ImmutablePathMatcher<List<HttpMatcher>> matcher : sharedPermissionsPathMatchers) {
                AuthenticationMechanism authMechanism = getAuthMechanism(routingContext, matcher);
                if (authMechanism != null) {
                    return authMechanism;
                }
            }
        }
        return getAuthMechanism(routingContext, pathMatcher);
    }

    /**
     * @deprecated This method is internal by nature, if you have a good use case, please report it
     *             so that we can document the use case and test it.
     */
    @Deprecated(forRemoval = true, since = "3.25")
    public String getAuthMechanismName(RoutingContext routingContext) {
        AuthenticationMechanism authenticationMechanism = getAuthMechanism(routingContext);
        if (authenticationMechanism != null) {
            return authenticationMechanism.name();
        }
        return null;
    }

    public boolean hasNoPermissions() {
        return hasNoPermissions;
    }

    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return checkPermissions(routingContext, identity, requestContext);
    }

    Uni<CheckResult> checkPermissions(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext, HttpSecurityPolicy... additionalPolicies) {
        final List<HttpSecurityPolicy> permissionCheckers = hasNoPermissions ? new ArrayList<>()
                : getHttpSecurityPolicies(routingContext);
        if (additionalPolicies.length > 0) {
            if (additionalPolicies.length == 1) {
                permissionCheckers.add(additionalPolicies[0]);
            } else {
                permissionCheckers.addAll(Arrays.asList(additionalPolicies));
            }
        }
        return doPermissionCheck(routingContext, identity, 0, null, permissionCheckers, requestContext);
    }

    private List<HttpSecurityPolicy> getHttpSecurityPolicies(RoutingContext routingContext) {
        final List<HttpSecurityPolicy> permissionCheckers;
        if (sharedPermissionsPathMatchers == null) {
            permissionCheckers = findPermissionCheckers(routingContext, pathMatcher);
        } else {
            permissionCheckers = new ArrayList<>();
            for (ImmutablePathMatcher<List<HttpMatcher>> matcher : sharedPermissionsPathMatchers) {
                permissionCheckers.addAll(findPermissionCheckers(routingContext, matcher));
            }
            permissionCheckers.addAll(findPermissionCheckers(routingContext, pathMatcher));
        }
        return permissionCheckers;
    }

    private Uni<CheckResult> doPermissionCheck(RoutingContext routingContext,
            Uni<SecurityIdentity> identity, int index, SecurityIdentity augmentedIdentity,
            List<HttpSecurityPolicy> permissionCheckers, AuthorizationRequestContext requestContext) {
        if (index == permissionCheckers.size()) {
            if (index > 0) {
                routingContext.put(PATH_MATCHING_POLICY_FOUND, true);
            }
            return Uni.createFrom().item(new CheckResult(true, augmentedIdentity));
        }
        //get the current checker
        HttpSecurityPolicy res = permissionCheckers.get(index);
        return res.checkPermission(routingContext, identity, requestContext)
                .flatMap(new Function<CheckResult, Uni<? extends CheckResult>>() {
                    @Override
                    public Uni<? extends CheckResult> apply(CheckResult checkResult) {
                        if (!checkResult.isPermitted()) {
                            if (checkResult.getAugmentedIdentity() == null) {
                                return CheckResult.deny();
                            } else {
                                return Uni.createFrom().item(new CheckResult(false, checkResult.getAugmentedIdentity()));
                            }
                        } else {
                            if (checkResult.getAugmentedIdentity() != null) {

                                //attempt to run the next checker
                                return doPermissionCheck(routingContext,
                                        checkResult.getAugmentedIdentityAsUni(), index + 1,
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

    private static AuthenticationMechanism getAuthMechanism(RoutingContext routingContext,
            ImmutablePathMatcher<List<HttpMatcher>> pathMatcher) {
        for (HttpMatcher i : findHttpMatchers(routingContext, pathMatcher)) {
            if (i.authMechanism != null) {
                return i.authMechanism;
            }
        }
        return null;
    }

    private static void addPermissionToPathMatcher(Map<String, HttpSecurityPolicy> permissionCheckers,
            HttpSecurityConfiguration.HttpPermissionCarrier httpPermission,
            ImmutablePathMatcher.ImmutablePathMatcherBuilder<List<HttpMatcher>> builder) {
        final HttpSecurityPolicy policy;
        if (httpPermission.getPolicy().instance() != null) {
            policy = httpPermission.getPolicy().instance();
        } else {
            String policyName = httpPermission.getPolicy().name();
            policy = permissionCheckers.get(policyName);
            if (policy == null) {
                throw new RuntimeException("Unable to find HTTP security policy " + policyName);
            }
        }

        for (String path : httpPermission.getPaths()) {
            HttpMatcher m = new HttpMatcher(httpPermission.getAuthMechanism(), httpPermission.getMethods(), policy);
            List<HttpMatcher> perms = new ArrayList<>();
            perms.add(m);
            builder.addPath(path, perms);
        }
    }

    private static List<HttpSecurityPolicy> findPermissionCheckers(RoutingContext context,
            ImmutablePathMatcher<List<HttpMatcher>> pathMatcher) {
        List<HttpSecurityPolicy> list = new ArrayList<>();
        for (HttpMatcher httpMatcher : findHttpMatchers(context, pathMatcher)) {
            list.add(httpMatcher.checker);
        }
        return list;
    }

    private static List<HttpMatcher> findHttpMatchers(RoutingContext context,
            ImmutablePathMatcher<List<HttpMatcher>> pathMatcher) {
        PathMatch<List<HttpMatcher>> toCheck = pathMatcher.match(context.normalizedPath());
        if (toCheck.getValue() == null || toCheck.getValue().isEmpty()) {
            return List.of();
        }
        List<HttpMatcher> methodMatch = new ArrayList<>();
        List<HttpMatcher> noMethod = new ArrayList<>();
        for (HttpMatcher i : toCheck.getValue()) {
            if (i.methods == null || i.methods.isEmpty()) {
                noMethod.add(i);
            } else if (i.methods.contains(context.request().method().toString())) {
                methodMatch.add(i);
            }
        }
        if (!methodMatch.isEmpty()) {
            return methodMatch;
        } else if (!noMethod.isEmpty()) {
            return noMethod;
        } else {
            //we deny if we did not match due to method filtering
            return List.of(HttpMatcher.DENY);
        }
    }

    static boolean policyApplied(RoutingContext routingContext) {
        return routingContext.get(PATH_MATCHING_POLICY_FOUND) != null;
    }

    private static Map<String, HttpSecurityPolicy> toNamedHttpSecPolicies(Map<String, PolicyConfig> rolePolicies,
            Instance<HttpSecurityPolicy> installedPolicies) {
        Map<String, HttpSecurityPolicy> namedPolicies = new HashMap<>();
        for (Instance.Handle<HttpSecurityPolicy> handle : installedPolicies.handles()) {
            if (handle.getBean().getBeanClass().getSuperclass() == AbstractPathMatchingHttpSecurityPolicy.class) {
                continue;
            }
            var policy = handle.get();
            if (policy.name() != null) {
                if (policy.name().isBlank()) {
                    throw new ConfigurationException("HTTP Security policy '" + policy + "' name must not be blank");
                }
                var previousPolicy = namedPolicies.put(policy.name(), policy);
                if (previousPolicy != null) {
                    throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, policy);
                }
            }
        }

        for (Map.Entry<String, PolicyConfig> e : rolePolicies.entrySet()) {
            final PolicyConfig policyConfig = e.getValue();
            final Map<String, Set<Permission>> roleToPermissions;
            if (policyConfig.permissions().isEmpty()) {
                roleToPermissions = null;
            } else {
                roleToPermissions = new HashMap<>();
                for (Map.Entry<String, List<String>> roleToPermissionStr : policyConfig.permissions().entrySet()) {

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
                        if (StringPermission.class.getName().equals(policyConfig.permissionClass())) {
                            permissions.add(new StringPermission(helper.permissionName, helper.actions.toArray(new String[0])));
                        } else {
                            permissions.add(customPermissionCreator(policyConfig, helper));
                        }
                    }

                    roleToPermissions.put(role, Set.copyOf(permissions));
                }
            }
            var rolesAllowedPolicy = new RolesAllowedHttpSecurityPolicy(policyConfig.rolesAllowed(), roleToPermissions,
                    policyConfig.roles());
            var previousPolicy = namedPolicies.put(e.getKey(), rolesAllowedPolicy);
            if (previousPolicy != null) {
                throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, rolesAllowedPolicy);
            }
        }

        var previousPolicy = namedPolicies.put(DenySecurityPolicy.NAME, DenySecurityPolicy.INSTANCE);
        if (previousPolicy != null) {
            throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, DenySecurityPolicy.INSTANCE);
        }
        previousPolicy = namedPolicies.put(PermitSecurityPolicy.NAME, new PermitSecurityPolicy());
        if (previousPolicy != null) {
            throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, new PermitSecurityPolicy());
        }
        previousPolicy = namedPolicies.put(AuthenticatedHttpSecurityPolicy.NAME, new AuthenticatedHttpSecurityPolicy());
        if (previousPolicy != null) {
            throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, new AuthenticatedHttpSecurityPolicy());
        }
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
            var constructor = loadClass(policyConfig.permissionClass()).getConstructors()[0];
            if (acceptsActions(policyConfig.permissionClass())) {
                return (Permission) constructor.newInstance(helper.permissionName, helper.actions.toArray(new String[0]));
            } else {
                return (Permission) constructor.newInstance(helper.permissionName);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to create Permission - class '%s', name '%s', actions '%s'",
                    policyConfig.permissionClass(), helper.permissionName,
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

    static ConfigurationException duplicateNamedPoliciesNotAllowedEx(HttpSecurityPolicy policy1, HttpSecurityPolicy policy2) {
        String policyClassName1 = ClientProxy.unwrap(policy1).getClass().getName();
        String policyClassName2 = ClientProxy.unwrap(policy2).getClass().getName();
        return new ConfigurationException("Only one HttpSecurityPolicy with the name '"
                + policy1.name() + "' is allowed, but found: " + policyClassName1 + " and " + policyClassName2);
    }

    record HttpMatcher(AuthenticationMechanism authMechanism, Set<String> methods, HttpSecurityPolicy checker) {
        private static final HttpMatcher DENY = new HttpMatcher(null, Set.of(), DenySecurityPolicy.INSTANCE);
    }
}
