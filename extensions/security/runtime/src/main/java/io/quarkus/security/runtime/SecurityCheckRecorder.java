package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.QuarkusSecurityRolesAllowedConfigBuilder.transformToKey;

import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.StringPermission;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermissionSecurityCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.SupplierRolesAllowedCheck;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.smallrye.config.Expressions;
import io.smallrye.config.common.utils.StringUtil;

@Recorder
public class SecurityCheckRecorder {

    private static volatile SecurityCheckStorage storage;
    private static final Set<SupplierRolesAllowedCheck> configExpRolesAllowedChecks = ConcurrentHashMap.newKeySet();

    public static SecurityCheckStorage getStorage() {
        return storage;
    }

    public SecurityCheck denyAll() {
        return DenyAllCheck.INSTANCE;
    }

    public SecurityCheck permitAll() {
        return PermitAllCheck.INSTANCE;
    }

    public SecurityCheck rolesAllowed(String... roles) {
        return RolesAllowedCheck.of(roles);
    }

    public SecurityCheck rolesAllowedSupplier(String[] allowedRoles, int[] configExpIndexes, int[] configKeys) {

        // here we add generated keys and values with the property expressions to the config source,
        // the config source will be registered with the Config system,
        // and we get all features available from Config
        for (int i = 0; i < configExpIndexes.length; i++) {
            QuarkusSecurityRolesAllowedConfigBuilder.addProperty(configKeys[i], allowedRoles[configExpIndexes[i]]);
        }

        final var check = new SupplierRolesAllowedCheck(
                resolveRolesAllowedConfigExp(allowedRoles, configExpIndexes, configKeys));
        configExpRolesAllowedChecks.add(check);
        return check;
    }

    /* STATIC INIT */
    public void recordRolesAllowedConfigExpression(String configExpression, int configKeyIndex,
            BiConsumer<String, Supplier<String[]>> configValueRecorder) {
        QuarkusSecurityRolesAllowedConfigBuilder.addProperty(configKeyIndex, configExpression);
        // one configuration expression resolves to string array because the expression can be list treated as list
        Supplier<String[]> configValSupplier = resolveRolesAllowedConfigExp(new String[] { configExpression },
                new int[] { 0 }, new int[] { configKeyIndex });
        configValueRecorder.accept(configExpression, configValSupplier);
    }

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

    public SecurityCheck authenticated() {
        return AuthenticatedCheck.INSTANCE;
    }

    /**
     * Creates {@link SecurityCheck} for a single permission.
     *
     * @return SecurityCheck
     */
    public SecurityCheck permissionsAllowed(Function<Object[], Permission> computedPermission,
            RuntimeValue<Permission> permissionRuntimeValue) {
        final Permission permission;
        if (computedPermission == null) {
            Objects.requireNonNull(permissionRuntimeValue);
            permission = permissionRuntimeValue.getValue();
        } else {
            permission = null;
        }
        return PermissionSecurityCheck.of(permission, computedPermission);
    }

    /**
     * Creates {@link SecurityCheck} for a permission set. User must have at least one of security check permissions.
     *
     * @return SecurityCheck
     */
    public SecurityCheck permissionsAllowed(List<Function<Object[], Permission>> computedPermissions,
            List<RuntimeValue<Permission>> permissionsRuntimeValue) {
        final Permission[] permissions;
        final Function<Object[], Permission[]> computedPermissionsAggregator;
        if (computedPermissions == null) {

            // plain permissions
            Objects.requireNonNull(permissionsRuntimeValue);
            computedPermissionsAggregator = null;
            permissions = new Permission[permissionsRuntimeValue.size()];
            for (int i = 0; i < permissionsRuntimeValue.size(); i++) {
                // assign permission
                permissions[i] = Objects.requireNonNull(permissionsRuntimeValue.get(i).getValue());
            }
        } else {

            // computed permissions
            permissions = null;
            computedPermissionsAggregator = new Function<>() {
                @Override
                public Permission[] apply(Object[] securedMethodParameters) {

                    // compute permissions
                    Permission[] result = new Permission[computedPermissions.size()];
                    for (int i = 0; i < computedPermissions.size(); i++) {
                        // instantiate Permission with actual method arguments
                        result[i] = computedPermissions.get(i).apply(securedMethodParameters);
                    }
                    return result;
                }
            };
        }

        return PermissionSecurityCheck.of(permissions, computedPermissionsAggregator);
    }

    /**
     * Creates {@link SecurityCheck} for a permission groups.
     * User must have at least one of security check permissions from each permission group.
     *
     * @return SecurityCheck
     */
    public SecurityCheck permissionsAllowedGroups(List<List<Function<Object[], Permission>>> computedPermissionGroups,
            List<List<RuntimeValue<Permission>>> permissionGroupsRuntimeValue) {
        final Function<Object[], Permission[][]> computedPermissionGroupAggregator;
        final Permission[][] permissionGroups;
        if (computedPermissionGroups == null) {

            // plain permission groups
            Objects.requireNonNull(permissionGroupsRuntimeValue);
            computedPermissionGroupAggregator = null;
            permissionGroups = new Permission[permissionGroupsRuntimeValue.size()][];

            // collect runtime values
            for (int i = 0; i < permissionGroupsRuntimeValue.size(); i++) {
                var groupRuntimeValue = permissionGroupsRuntimeValue.get(i);
                permissionGroups[i] = new Permission[groupRuntimeValue.size()];
                for (int j = 0; j < groupRuntimeValue.size(); j++) {
                    // assign permission
                    permissionGroups[i][j] = groupRuntimeValue.get(j).getValue();
                }
            }
        } else {

            // computed permission groups
            permissionGroups = null;
            computedPermissionGroupAggregator = new Function<>() {
                @Override
                public Permission[][] apply(Object[] securedMethodParams) {

                    // compute permissions
                    Permission[][] permissionGroups = new Permission[computedPermissionGroups.size()][];
                    for (int i = 0; i < computedPermissionGroups.size(); i++) {
                        var computedPermissionGroup = computedPermissionGroups.get(i);
                        permissionGroups[i] = new Permission[computedPermissionGroup.size()];
                        for (int j = 0; j < computedPermissionGroup.size(); j++) {
                            // instantiate Permission with actual method arguments
                            permissionGroups[i][j] = computedPermissionGroup.get(j).apply(securedMethodParams);
                        }
                    }

                    return permissionGroups;
                }
            };
        }

        return PermissionSecurityCheck.of(permissionGroups, computedPermissionGroupAggregator);
    }

    public Function<Object[], Permission> toComputedPermission(RuntimeValue<Permission> permissionRuntimeVal) {
        return new Function<>() {
            @Override
            public Permission apply(Object[] objects) {
                return permissionRuntimeVal.getValue();
            }
        };
    }

    public RuntimeValue<Permission> createStringPermission(String name, String[] actions) {
        return new RuntimeValue<>(new StringPermission(name, actions));
    }

    /**
     * Creates permission.
     *
     * @param name permission name
     * @param clazz permission class
     * @param actions nullable actions
     * @param passActionsToConstructor flag signals whether Permission constructor accepts (name) or (name, actions)
     * @return {@link RuntimeValue<Permission>}
     */
    public RuntimeValue<Permission> createPermission(String name, String clazz, String[] actions,
            boolean passActionsToConstructor) {
        final Permission permission;
        try {
            if (passActionsToConstructor) {
                permission = (Permission) loadClass(clazz).getConstructors()[0].newInstance(name, actions);
            } else {
                permission = (Permission) loadClass(clazz).getConstructors()[0].newInstance(name);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to create Permission - class '%s', name '%s', actions '%s'", clazz,
                    name, Arrays.toString(actions)), e);
        }
        return new RuntimeValue<>(permission);
    }

    /**
     * Creates function that transform arguments of a method annotated with {@link io.quarkus.security.PermissionsAllowed}
     * to custom {@link Permission}.
     *
     * @param permissionName permission name
     * @param clazz permission class
     * @param actions permission actions
     * @param passActionsToConstructor flag signals whether Permission constructor accepts (name) or (name, actions)
     * @param formalParamIndexes indexes of secured method params that should be passed to permission constructor
     * @return computed permission
     */
    public Function<Object[], Permission> createComputedPermission(String permissionName, String clazz, String[] actions,
            boolean passActionsToConstructor, int[] formalParamIndexes) {
        final int addActions = (passActionsToConstructor ? 1 : 0);
        final int argsCount = 1 + addActions + formalParamIndexes.length;
        final int methodArgsStart = 1 + addActions;
        final var permissionClassConstructor = loadClass(clazz).getConstructors()[0];
        return new Function<>() {
            @Override
            public Permission apply(Object[] securedMethodArgs) {
                try {
                    final Object[] initArgs = initArgs(securedMethodArgs);
                    return (Permission) permissionClassConstructor.newInstance(initArgs);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(
                            String.format("Failed to create computed Permission - class '%s', name '%s', actions '%s', ", clazz,
                                    permissionName, Arrays.toString(actions)),
                            e);
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
                    initArgs[methodArgsStart + i] = methodArgs[formalParamIndexes[i]];
                }
                return initArgs;
            }
        };
    }

    public RuntimeValue<SecurityCheckStorageBuilder> newBuilder() {
        return new RuntimeValue<>(new SecurityCheckStorageBuilder());
    }

    public void addMethod(RuntimeValue<SecurityCheckStorageBuilder> builder, String className,
            String methodName,
            String[] parameterTypes,
            SecurityCheck securityCheck) {
        builder.getValue().registerCheck(className, methodName, parameterTypes, securityCheck);
    }

    public void create(RuntimeValue<SecurityCheckStorageBuilder> builder) {
        storage = builder.getValue().create();
    }

    public void resolveRolesAllowedConfigExpRoles() {
        if (!configExpRolesAllowedChecks.isEmpty()) {
            for (SupplierRolesAllowedCheck configExpRolesAllowedCheck : configExpRolesAllowedChecks) {
                configExpRolesAllowedCheck.resolveAllowedRoles();
            }
            configExpRolesAllowedChecks.clear();
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for creating permission", e);
        }
    }

    public void registerDefaultSecurityCheck(RuntimeValue<SecurityCheckStorageBuilder> builder, SecurityCheck securityCheck) {
        builder.getValue().registerDefaultSecurityCheck(securityCheck);
    }
}
