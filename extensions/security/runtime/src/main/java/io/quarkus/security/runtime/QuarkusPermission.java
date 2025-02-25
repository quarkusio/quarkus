package io.quarkus.security.runtime;

import java.security.Permission;
import java.util.Objects;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;

/**
 * Special type of {@link Permission} which is used by Quarkus Security to call CDI bean methods annotated
 * with the {@link PermissionChecker}.
 */
public abstract class QuarkusPermission<T> extends Permission {

    private volatile InstanceHandle<T> bean = null;

    /**
     * Subclasses can declare constructors that accept permission name and/or arguments of a secured method.
     *
     * @param permissionName permission name, this matches {@link PermissionChecker#value()}
     * @see PermissionsAllowed#params() for more information about additional Permission arguments
     */
    protected QuarkusPermission(String permissionName) {
        super(permissionName);
    }

    /**
     * @return declaring class of the method annotated with the {@link PermissionChecker}
     */
    protected abstract Class<T> getBeanClass();

    /**
     * @return true if {@link #isGranted(SecurityIdentity)} must be executed on a worker thread
     */
    protected abstract boolean isBlocking();

    /**
     * Whether user-defined permission checker returns {@link Uni}.
     *
     * @return true if {@link #isGrantedUni(SecurityIdentity)} should be used instead of the
     *         {@link #isGranted(SecurityIdentity)}
     */
    protected abstract boolean isReactive();

    /**
     * @return CDI bean that declares the method annotated with the {@link PermissionChecker}
     */
    protected final T getBean() {
        return getBeanInstanceHandle().get();
    }

    /**
     * Determines whether access to secured resource should be granted in a synchronous manner.
     * Subclasses should override this method unless they need to perform permission check in an asynchronous manner.
     *
     * @param securityIdentity {@link SecurityIdentity}
     * @return true if access should be granted and false otherwise
     */
    protected abstract boolean isGranted(SecurityIdentity securityIdentity);

    /**
     * Determines whether access to secured resource should be granted in an asynchronous manner.
     * Subclasses can override this method, however it is only called when {@link #isReactive()} returns {@code true}.
     *
     * @param securityIdentity {@link SecurityIdentity}
     * @return Uni with {@code true} if access should be granted and Uni with {@code false} otherwise
     */
    protected abstract Uni<Boolean> isGrantedUni(SecurityIdentity securityIdentity);

    final Uni<Boolean> isGranted(SecurityIdentity identity, BlockingSecurityExecutor blockingExecutor) {
        if (isBlocking()) {
            return blockingExecutor.executeBlocking(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return isGranted(identity);
                }
            });
        }

        try {
            if (isReactive()) {
                return isGrantedUni(identity);
            } else {
                return Uni.createFrom().item(isGranted(identity));
            }
        } catch (Throwable throwable) {
            return Uni.createFrom().failure(throwable);
        }
    }

    /**
     * @throws IllegalStateException for this permission can only be set to the {@link PermissionsAllowed#permission()}
     */
    @Override
    public final boolean implies(Permission requiredPermission) {
        // possessed permission implies required permission
        // this is required permission, not the possessed one
        throw new IllegalStateException("QuarkusPermission should never be assigned to a SecurityIdentity. "
                + "This permission can only be set to the @PermissionsAllowed#permission attribute by Quarkus itself.");
    }

    @Override
    public final String getActions() {
        return "";
    }

    @Override
    public final boolean equals(Object object) {
        return this == object;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(toString());
    }

    private InstanceHandle<T> getBeanInstanceHandle() {
        if (bean == null) {
            // this is done lazily because permissions without extra constructor arguments are created before Arc is ready
            bean = Arc.container().instance(getBeanClass());
            if (!bean.isAvailable()) {
                throw new IllegalStateException(
                        "CDI bean '%s' is not available, but it is required by the @PermissionChecker method"
                                .formatted(getBeanClass()));
            }
        }
        return bean;
    }

    // used by generated subclasses
    protected static Uni<Boolean> accessDenied() {
        return Uni.createFrom().item(false);
    }
}
