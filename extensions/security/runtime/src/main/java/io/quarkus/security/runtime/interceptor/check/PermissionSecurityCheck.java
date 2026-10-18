package io.quarkus.security.runtime.interceptor.check;

import static java.lang.Boolean.TRUE;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Objects;
import java.util.function.Function;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.smallrye.mutiny.Uni;

public abstract class PermissionSecurityCheck<T> implements SecurityCheck {

    private static final Uni<Object> SUCCESSFUL_CHECK = Uni.createFrom().nullItem();
    private final T permissions;
    private final Function<Object[], T> computedPermissions;
    private final boolean useComputedPermissions;

    private PermissionSecurityCheck(T permissions, Function<Object[], T> computedPermissions) {
        if (permissions == null) {
            Objects.requireNonNull(computedPermissions);
            this.useComputedPermissions = true;
        } else {
            if (computedPermissions == null) {
                this.useComputedPermissions = false;
            } else {
                throw new IllegalStateException("PermissionSecurityCheck must be created either for computed permissions" +
                        "or plain permissions, but received both");
            }
        }
        this.permissions = permissions;
        this.computedPermissions = computedPermissions;
    }

    private T getPermissions(Object[] parameters) {
        if (useComputedPermissions) {
            return computedPermissions.apply(parameters);
        }
        return permissions;
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        checkPermissions(identity, getPermissions(parameters));
    }

    @Override
    public void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        checkPermissions(identity, getPermissions(parameters));
    }

    @Override
    public Uni<?> nonBlockingApply(SecurityIdentity identity, Method method, Object[] parameters) {
        return checkPermissions(identity, getPermissions(parameters), 0);
    }

    @Override
    public Uni<?> nonBlockingApply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        return checkPermissions(identity, getPermissions(parameters), 0);
    }

    @Override
    public boolean requiresMethodArguments() {
        return useComputedPermissions;
    }

    private static void throwException(SecurityIdentity identity) {
        throw getException(identity);
    }

    private static RuntimeException getException(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            return new UnauthorizedException();
        } else {
            return new ForbiddenException();
        }
    }

    protected abstract Uni<?> checkPermissions(SecurityIdentity identity, T permissions, int i);

    protected abstract void checkPermissions(SecurityIdentity identity, T permissions);

    /**
     * Creates permission check with a single permission. Either {@code permission} or {@code computedPermission}
     * must not be null.
     *
     * @param permission Permission
     * @param computedPermission the function that is invoked every single time permission is checked with request or
     *        method parameters
     * @return created {@link SecurityCheck}
     */
    public static SecurityCheck of(Permission permission, Function<Object[], Permission> computedPermission) {
        return new PermissionSecurityCheck<>(permission, computedPermission) {
            @Override
            protected Uni<?> checkPermissions(SecurityIdentity identity, Permission permission, int i) {
                return identity
                        .checkPermission(permission)
                        .onItem()
                        .transformToUni(new Function<>() {
                            @Override
                            public Uni<?> apply(Boolean hasPermission) {
                                if (TRUE.equals(hasPermission)) {
                                    return SUCCESSFUL_CHECK;
                                }

                                // check failed
                                return Uni.createFrom().failure(getException(identity));
                            }
                        });
            }

            @Override
            protected void checkPermissions(SecurityIdentity identity, Permission permission) {
                if (!identity.checkPermissionBlocking(permission)) {
                    throwException(identity);
                }
            }
        };
    }

    /**
     * Creates permission check with permissions. Permission check will be successful if {@link SecurityIdentity} has
     * at least one of permissions. Either {@code permission} or {@code computedPermission} must not be null.
     *
     * @param permissions Permission[]
     * @param computedPermissions the function that is invoked every single time permissions are checked with request or
     *        method parameters
     * @return created {@link SecurityCheck}
     */
    public static SecurityCheck of(Permission[] permissions, Function<Object[], Permission[]> computedPermissions) {
        return new PermissionSecurityCheck<>(permissions, computedPermissions) {
            @Override
            protected Uni<?> checkPermissions(SecurityIdentity identity, Permission[] permissions, int i) {
                // security identity must have at least one of required permissions
                return PermissionSecurityCheck.checkPermissions(identity, permissions, i);
            }

            @Override
            protected void checkPermissions(SecurityIdentity identity, Permission[] permissions) {
                for (Permission permission : permissions) {
                    if (identity.checkPermissionBlocking(permission)) {
                        // success - security identity has at least one of required permissions
                        return;
                    }
                }
                throwException(identity);
            }
        };
    }

    /**
     * Creates permission check with permission groups. Permission check will be successful if {@link SecurityIdentity}
     * has at least one of permissions of each permission group. Either {@code permission} or {@code computedPermission}
     * must not be null.
     *
     * @param permissions array of permission groups
     * @param computedPermissions the function that is invoked every single time permissions are checked with request or
     *        method parameters
     * @return created {@link SecurityCheck}
     */
    public static SecurityCheck of(Permission[][] permissions, Function<Object[], Permission[][]> computedPermissions) {
        return new PermissionSecurityCheck<>(permissions, computedPermissions) {
            @Override
            protected Uni<?> checkPermissions(SecurityIdentity identity, Permission[][] permissionGroups, int i) {
                // check that identity has at least one permission from each permission group
                return PermissionSecurityCheck.checkPermissions(identity, permissionGroups[i], 0)
                        .onItem()
                        .transformToUni(new Function<Object, Uni<?>>() {
                            @Override
                            public Uni<?> apply(Object o) {
                                if (i + 1 < permissionGroups.length) {
                                    // check next permission group
                                    return checkPermissions(identity, permissionGroups, i + 1);
                                }

                                return SUCCESSFUL_CHECK;
                            }
                        });
            }

            @Override
            protected void checkPermissions(SecurityIdentity identity, Permission[][] permissionGroups) {
                // logical AND between permission groups (must have at least one permission from each group)
                groupBlock: for (Permission[] permissionGroup : permissionGroups) {

                    // logical OR between permissions
                    for (Permission permission : permissionGroup) {
                        if (identity.checkPermissionBlocking(permission)) {
                            // success - check next permission group
                            continue groupBlock;
                        }
                    }

                    // must have at least one of 'OR' permissions
                    throwException(identity);
                }
            }
        };
    }

    private static Uni<?> checkPermissions(SecurityIdentity identity, Permission[] permissions, int i) {
        // recursive check that the identity has at least one of required permissions
        return identity
                .checkPermission(permissions[i])
                .onItem()
                .transformToUni(new Function<>() {
                    @Override
                    public Uni<?> apply(Boolean hasPermission) {
                        if (TRUE.equals(hasPermission)) {
                            return SUCCESSFUL_CHECK;
                        } else {
                            final boolean hasAnotherPermission = i + 1 < permissions.length;
                            if (!hasAnotherPermission) {
                                // check failed
                                return Uni.createFrom().failure(getException(identity));
                            }

                            // check next permission
                            return checkPermissions(identity, permissions, i + 1);
                        }
                    }
                });
    }

}
