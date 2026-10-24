package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.QuarkusSecurityRolesAllowedConfigBuilder.transformToKey;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageImpl;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.ConfigExpressionRolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.MethodSecurityCheckSpec;
import io.quarkus.security.runtime.interceptor.check.PermissionSecurityCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.SecurityCheckSpec;
import io.quarkus.security.runtime.interceptor.check.SecurityCheckSpec.PermissionSpec;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.RolesAllowedConfigExpressions;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckContribution;
import io.smallrye.config.Expressions;
import io.smallrye.config.common.utils.StringUtil;

/**
 * Creates {@link SecurityCheck} instances from their build-time {@link SecurityCheckSpec descriptions}.
 * The factory methods are invoked by service actions during the static init. The factory that creates the security
 * check storage is a static-init service, so that the configuration expressions of the checks it creates can be
 * resolved once the runtime configuration is available.
 */
public final class SecurityCheckFactory {

    private static final Logger LOGGER = Logger.getLogger(SecurityCheckFactory.class);

    /**
     * Name of the class generated at build time that holds the permission constructor parameter converters.
     */
    public static final String PERMISSION_METHOD_CONVERTER_CLASS = "io.quarkus.security.runtime.PermissionMethodConverter";

    private final Map<SecurityCheckSpec, SecurityCheck> checks = new HashMap<>();
    private final Map<PermissionSpec, Permission> permissions = new HashMap<>();
    private final Map<String, MethodHandle> converters = new HashMap<>();
    private final List<ConfigExpressionRolesAllowedCheck> configExpressionChecks = new ArrayList<>();

    public SecurityCheckFactory() {
    }

    /**
     * Creates a single security check.
     */
    public static SecurityCheck create(SecurityCheckSpec spec) {
        return new SecurityCheckFactory().createCheck(spec);
    }

    /**
     * Creates the security check storage. Exactly one {@link SecurityCheck} instance is created for each distinct
     * {@link SecurityCheckSpec}.
     *
     * @param methodChecks checks of the methods secured with the security annotations
     * @param defaultCheck check applied to methods without a security check, may be {@code null}
     * @param contributions checks contributed by other extensions, keyed by the contribution service name; a
     *        contributed check replaces the check created from the security annotations of the method
     * @param securityCheckServices all the {@link SecurityCheck} services keyed by their service name, that is the
     *        class-level checks; their configuration expressions are resolved eagerly as well
     */
    public SecurityCheckStorageImpl createStorage(List<MethodSecurityCheckSpec> methodChecks,
            SecurityCheckSpec defaultCheck, Map<String, SecurityCheckContribution> contributions,
            Map<String, SecurityCheck> securityCheckServices) {
        Map<MethodDescription, SecurityCheck> result = new HashMap<>();
        for (MethodSecurityCheckSpec methodCheck : methodChecks) {
            var method = new MethodDescription(methodCheck.className(), methodCheck.methodName(),
                    methodCheck.parameterTypes().toArray(String[]::new));
            result.put(method, createCheck(methodCheck.check()));
        }
        Map<MethodDescription, String> contributors = new HashMap<>();
        for (Map.Entry<String, SecurityCheckContribution> contribution : contributions.entrySet()) {
            for (Map.Entry<MethodDescription, SecurityCheck> entry : contribution.getValue().securityChecks().entrySet()) {
                String previousContributor = contributors.put(entry.getKey(), contribution.getKey());
                if (previousContributor != null) {
                    throw new IllegalStateException(
                            "The security check of the method '%s#%s' is contributed by both '%s' and '%s'".formatted(
                                    entry.getKey().getClassName(), entry.getKey().getMethodName(), previousContributor,
                                    contribution.getKey()));
                }
                result.put(entry.getKey(), entry.getValue());
            }
        }
        for (SecurityCheck securityCheckService : securityCheckServices.values()) {
            if (securityCheckService instanceof ConfigExpressionRolesAllowedCheck configExpressionCheck) {
                configExpressionChecks.add(configExpressionCheck);
            }
        }
        SecurityCheck defaultSecurityCheck = defaultCheck == null ? null : createCheck(defaultCheck);
        return new SecurityCheckStorageImpl(result, defaultSecurityCheck);
    }

    /**
     * Eagerly resolves the configuration expressions used in the {@code @RolesAllowed} annotations of the checks
     * created by this factory, so that failures are reported when the application starts rather than when the first
     * request is checked. Must be called when the runtime configuration is available.
     */
    public void resolveRolesAllowedConfigExpressions() {
        for (ConfigExpressionRolesAllowedCheck check : configExpressionChecks) {
            check.resolveAllowedRoles();
        }
    }

    private SecurityCheck createCheck(SecurityCheckSpec spec) {
        SecurityCheck existing = checks.get(spec);
        if (existing != null) {
            return existing;
        }
        SecurityCheck check = switch (spec) {
            case SecurityCheckSpec.Standard standard -> switch (standard) {
                case PERMIT_ALL -> PermitAllCheck.INSTANCE;
                case DENY_ALL -> DenyAllCheck.INSTANCE;
                case AUTHENTICATED -> AuthenticatedCheck.INSTANCE;
            };
            case SecurityCheckSpec.RolesAllowed rolesAllowed -> createRolesAllowedCheck(rolesAllowed);
            case SecurityCheckSpec.PermissionsAllowed permissionsAllowed -> createPermissionsAllowedCheck(permissionsAllowed);
        };
        checks.put(spec, check);
        return check;
    }

    private SecurityCheck createRolesAllowedCheck(SecurityCheckSpec.RolesAllowed spec) {
        String[] allowedRoles = spec.roles().toArray(String[]::new);
        if (!spec.hasConfigExpressions()) {
            return RolesAllowedCheck.of(allowedRoles);
        }
        int[] configExpIndexes = toIntArray(spec.configExpressionIndexes());
        // the expressions are registered with the roles allowed config source under these keys by a separate service,
        // so we get all features available from Config when the expressions are resolved
        int[] configKeys = toIntArray(spec.configKeys());

        var check = new ConfigExpressionRolesAllowedCheck(
                resolveRolesAllowedConfigExp(allowedRoles, configExpIndexes, configKeys));
        configExpressionChecks.add(check);
        return check;
    }

    /**
     * Creates the suppliers of the roles resolved from the configuration expressions that other extensions accept
     * in place of role names.
     *
     * @param configExpressionKeys the artificial configuration keys under which the expressions are registered
     *        with the roles allowed configuration source, keyed by expression
     */
    public static RolesAllowedConfigExpressions createRolesAllowedConfigExpressions(
            Map<String, Integer> configExpressionKeys) {
        Map<String, Supplier<String[]>> rolesSuppliers = new HashMap<>();
        for (Map.Entry<String, Integer> entry : configExpressionKeys.entrySet()) {
            String configExpression = entry.getKey();
            // one configuration expression resolves to string array because the expression can be list treated as list
            rolesSuppliers.put(configExpression, resolveRolesAllowedConfigExp(new String[] { configExpression },
                    new int[] { 0 }, new int[] { entry.getValue() }));
        }
        return new RolesAllowedConfigExpressions(rolesSuppliers);
    }

    /**
     * Creates a supplier of the allowed roles that resolves the configuration expressions among them.
     */
    private static Supplier<String[]> resolveRolesAllowedConfigExp(String[] allowedRoles, int[] configExpIndexes,
            int[] configKeys) {

        final List<String> roles = new ArrayList<>(Arrays.asList(allowedRoles));
        return new Supplier<String[]>() {
            @Override
            public String[] get() {
                final var config = ConfigProviderResolver.instance().getConfig(Thread.currentThread().getContextClassLoader());
                if (config.getOptionalValue(Config.PROPERTY_EXPRESSIONS_ENABLED, Boolean.class).orElse(Boolean.TRUE)
                        && Expressions.isEnabled()) {
                    // property expressions are enabled
                    for (int i = 0; i < configExpIndexes.length; i++) {
                        // resolve configuration expressions specified as value of the @RolesAllowed annotation
                        var strVal = config.getValue(transformToKey(configKeys[i]), String.class);

                        // treat config value that contains collection separator as a list
                        // @RolesAllowed({"${my.roles}"}) => my.roles=one,two <=> @RolesAllowed({"one", "two"})
                        if (strVal != null && strVal.contains(",")) {
                            var strArr = StringUtil.split(strVal);
                            if (strArr.length >= 1) {
                                // role order is irrelevant as logical operator between them is OR

                                // first role will go to the original place, double escaped comma will be parsed correctly
                                strVal = strArr[0];

                                if (strArr.length > 1) {
                                    // the rest of the roles will be appended at the end
                                    for (int i1 = 1; i1 < strArr.length; i1++) {
                                        roles.add(strArr[i1]);
                                    }
                                }
                            }
                        }

                        roles.set(configExpIndexes[i], strVal);
                    }
                }
                return roles.toArray(String[]::new);
            }
        };
    }

    private SecurityCheck createPermissionsAllowedCheck(SecurityCheckSpec.PermissionsAllowed spec) {
        List<List<PermissionSpec>> permissionGroups = spec.permissionGroups();
        if (permissionGroups.size() == 1) {
            List<PermissionSpec> permissionGroup = permissionGroups.get(0);
            if (permissionGroup.size() == 1) {

                // single permission
                PermissionSpec permissionSpec = permissionGroup.get(0);
                if (permissionSpec.computed()) {
                    return PermissionSecurityCheck.of((Permission) null, createComputedPermission(permissionSpec));
                }
                return PermissionSecurityCheck.of(createPermission(permissionSpec), (Function<Object[], Permission>) null);
            }

            // multiple OR operands (permission OR permission OR ...)
            if (spec.computed()) {
                List<Function<Object[], Permission>> computedPermissions = asComputedPermissions(permissionGroup);
                Function<Object[], Permission[]> computedPermissionsAggregator = new Function<>() {
                    @Override
                    public Permission[] apply(Object[] securedMethodParameters) {
                        return computePermissions(computedPermissions, securedMethodParameters);
                    }
                };
                return PermissionSecurityCheck.of((Permission[]) null, computedPermissionsAggregator);
            }
            return PermissionSecurityCheck.of(asPermissions(permissionGroup), (Function<Object[], Permission[]>) null);
        }

        // permission group AND permission group AND permission group AND ...
        // permission group = (permission OR permission OR permission OR ...)
        if (spec.computed()) {
            List<List<Function<Object[], Permission>>> computedPermissionGroups = new ArrayList<>(permissionGroups.size());
            for (List<PermissionSpec> permissionGroup : permissionGroups) {
                computedPermissionGroups.add(asComputedPermissions(permissionGroup));
            }
            Function<Object[], Permission[][]> computedPermissionGroupAggregator = new Function<>() {
                @Override
                public Permission[][] apply(Object[] securedMethodParams) {
                    Permission[][] result = new Permission[computedPermissionGroups.size()][];
                    for (int i = 0; i < computedPermissionGroups.size(); i++) {
                        result[i] = computePermissions(computedPermissionGroups.get(i), securedMethodParams);
                    }
                    return result;
                }
            };
            return PermissionSecurityCheck.of((Permission[][]) null, computedPermissionGroupAggregator);
        }
        Permission[][] plainPermissionGroups = new Permission[permissionGroups.size()][];
        for (int i = 0; i < permissionGroups.size(); i++) {
            plainPermissionGroups[i] = asPermissions(permissionGroups.get(i));
        }
        return PermissionSecurityCheck.of(plainPermissionGroups, (Function<Object[], Permission[][]>) null);
    }

    private static Permission[] computePermissions(List<Function<Object[], Permission>> computedPermissions,
            Object[] securedMethodParameters) {
        Permission[] result = new Permission[computedPermissions.size()];
        for (int i = 0; i < computedPermissions.size(); i++) {
            // instantiate Permission with actual method arguments
            result[i] = computedPermissions.get(i).apply(securedMethodParameters);
        }
        return result;
    }

    private List<Function<Object[], Permission>> asComputedPermissions(List<PermissionSpec> permissionGroup) {
        List<Function<Object[], Permission>> computedPermissions = new ArrayList<>(permissionGroup.size());
        for (PermissionSpec permissionSpec : permissionGroup) {
            if (permissionSpec.computed()) {
                computedPermissions.add(createComputedPermission(permissionSpec));
            } else {
                // make permission computed for we can't combine computed and plain permissions (to keep things simple)
                Permission permission = createPermission(permissionSpec);
                computedPermissions.add(new Function<>() {
                    @Override
                    public Permission apply(Object[] objects) {
                        return permission;
                    }
                });
            }
        }
        return List.copyOf(computedPermissions);
    }

    private Permission[] asPermissions(List<PermissionSpec> permissionGroup) {
        Permission[] permissions = new Permission[permissionGroup.size()];
        for (int i = 0; i < permissionGroup.size(); i++) {
            permissions[i] = createPermission(permissionGroup.get(i));
        }
        return permissions;
    }

    private Permission createPermission(PermissionSpec spec) {
        Permission permission = permissions.get(spec);
        if (permission == null) {
            permission = newPermission(spec);
            permissions.put(spec, permission);
        }
        return permission;
    }

    private static Permission newPermission(PermissionSpec spec) {
        String[] actions = toActions(spec);
        if (StringPermission.class.getName().equals(spec.permissionClass())) {
            return new StringPermission(spec.name(), actions);
        }
        try {
            if (spec.passActionsToConstructor()) {
                return (Permission) loadClass(spec.permissionClass()).getConstructors()[0].newInstance(spec.name(), actions);
            } else {
                return (Permission) loadClass(spec.permissionClass()).getConstructors()[0].newInstance(spec.name());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
            LOGGER.errorf(e, "Failed to create Permission - class '%s', name '%s', actions '%s', access will be denied",
                    spec.permissionClass(), spec.name(), Arrays.toString(actions));
            throw new ForbiddenException();
        }
    }

    /**
     * Creates function that transform arguments of a method annotated with {@link io.quarkus.security.PermissionsAllowed}
     * to custom {@link Permission}.
     */
    private Function<Object[], Permission> createComputedPermission(PermissionSpec spec) {
        final String permissionName = spec.name();
        final String clazz = spec.permissionClass();
        final String[] actions = toActions(spec);
        final boolean passActionsToConstructor = spec.passActionsToConstructor();
        // indexes of secured method params that should be passed to permission constructor
        final int[] formalParamIndexes = toIntArray(spec.formalParamIndexes());
        // converts method parameter to constructor parameter; most of the time, this will be
        // either identity function (null converter) or a method calling method parameter getter
        final MethodHandle[] formalParamConverters = new MethodHandle[formalParamIndexes.length];
        for (int i = 0; i < formalParamIndexes.length; i++) {
            String converterName = spec.formalParamConverters().get(i);
            formalParamConverters[i] = converterName.isEmpty() ? null : converter(converterName);
        }
        final int addActions = (passActionsToConstructor ? 1 : 0);
        final int argsCount = 1 + addActions + formalParamIndexes.length;
        final int methodArgsStart = 1 + addActions;
        final Constructor<?> permissionClassConstructor = loadClass(clazz).getConstructors()[0];
        return new Function<>() {
            @Override
            public Permission apply(Object[] securedMethodArgs) {
                try {
                    final Object[] initArgs = initArgs(securedMethodArgs);
                    return (Permission) permissionClassConstructor.newInstance(initArgs);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException e) {
                    LOGGER.errorf(e,
                            "Failed to create computed Permission - class '%s', name '%s', actions '%s', access will be denied",
                            clazz, permissionName, Arrays.toString(actions));
                    throw new ForbiddenException();
                }
            }

            private Object[] initArgs(Object[] methodArgs) {
                // Permission constructor init args are: permission name, possibly actions, selected secured method args
                final Object[] initArgs = new Object[argsCount];
                initArgs[0] = permissionName;
                if (passActionsToConstructor) {
                    initArgs[1] = actions;
                }
                for (int i = 0; i < formalParamIndexes.length; i++) {
                    var methodArg = methodArgs[formalParamIndexes[i]];
                    if (formalParamConverters[i] == null) {
                        initArgs[methodArgsStart + i] = methodArg;
                    } else {
                        initArgs[methodArgsStart + i] = convertMethodParamToPermParam(formalParamConverters[i], methodArg);
                    }
                }
                return initArgs;
            }
        };
    }

    private MethodHandle converter(String converterName) {
        MethodHandle converter = converters.get(converterName);
        if (converter == null) {
            try {
                converter = MethodHandles.publicLookup().findStatic(loadClass(PERMISSION_METHOD_CONVERTER_CLASS),
                        converterName, MethodType.methodType(Object.class, Object.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException("Failed to create Permission constructor method parameter converter", e);
            }
            converters.put(converterName, converter);
        }
        return converter;
    }

    private static Object convertMethodParamToPermParam(MethodHandle converter, Object methodArg) {
        try {
            return (Object) converter.invokeExact(methodArg);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "Failed to convert method argument '%s' to Permission constructor parameter".formatted(methodArg), e);
        }
    }

    private static String[] toActions(PermissionSpec spec) {
        return spec.actions() == null ? null : spec.actions().toArray(String[]::new);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for creating permission", e);
        }
    }
}
