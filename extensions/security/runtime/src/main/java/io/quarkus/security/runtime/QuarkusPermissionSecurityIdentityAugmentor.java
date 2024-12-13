package io.quarkus.security.runtime;

import java.security.Permission;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;

/**
 * Adds a permission checker that grants access to the {@link QuarkusPermission}
 * when {@link QuarkusPermission#isGranted(SecurityIdentity)} is true.
 */
public final class QuarkusPermissionSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    /**
     * Permission checker only authorizes authenticated users and checkers shouldn't throw a security exception.
     * However, it can happen than runtime exception occur, and we shouldn't leak that something wrong with response status.
     */
    private static final Predicate<Throwable> NOT_A_FORBIDDEN_EXCEPTION = new Predicate<>() {
        @Override
        public boolean test(Throwable throwable) {
            return !(throwable instanceof ForbiddenException);
        }
    };
    private static final Function<Throwable, Throwable> WRAP_WITH_FORBIDDEN_EXCEPTION = new Function<>() {
        @Override
        public Throwable apply(Throwable throwable) {
            return new ForbiddenException(throwable);
        }
    };

    private final BlockingSecurityExecutor blockingExecutor;

    QuarkusPermissionSecurityIdentityAugmentor(BlockingSecurityExecutor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        return Uni.createFrom().item(QuarkusSecurityIdentity
                .builder(identity)
                .addPermissionChecker(new Function<>() {
                    @Override
                    public Uni<Boolean> apply(Permission requiredpermission) {
                        if (requiredpermission instanceof QuarkusPermission<?> quarkusPermission) {
                            return quarkusPermission
                                    .isGranted(identity, blockingExecutor)
                                    .onFailure(NOT_A_FORBIDDEN_EXCEPTION).transform(WRAP_WITH_FORBIDDEN_EXCEPTION);
                        }
                        return Uni.createFrom().item(false);
                    }
                })
                .build());
    }

    @Override
    public int priority() {
        // we do not rely on this value and always add this augmentor as the last one manually
        return Integer.MAX_VALUE;
    }
}
