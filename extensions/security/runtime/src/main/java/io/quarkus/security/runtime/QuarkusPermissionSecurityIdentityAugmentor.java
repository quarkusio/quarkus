package io.quarkus.security.runtime;

import java.security.Permission;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.credential.Credential;
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
    // keep in sync with HttpSecurityUtils
    private static final String ROUTING_CONTEXT_ATTRIBUTE = "quarkus.http.routing.context";

    private final BiFunction<SecurityIdentity, Permission, Uni<Boolean>> permissionChecker;

    QuarkusPermissionSecurityIdentityAugmentor(BlockingSecurityExecutor blockingExecutor) {
        this.permissionChecker = new BiFunction<SecurityIdentity, Permission, Uni<Boolean>>() {
            @Override
            public Uni<Boolean> apply(SecurityIdentity finalIdentity, Permission requiredpermission) {
                if (requiredpermission instanceof QuarkusPermission<?> quarkusPermission) {
                    return quarkusPermission
                            .isGranted(finalIdentity, blockingExecutor)
                            .onFailure(NOT_A_FORBIDDEN_EXCEPTION).transform(WRAP_WITH_FORBIDDEN_EXCEPTION);
                }
                return Uni.createFrom().item(false);
            }
        };
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        return Uni.createFrom().item(
                new PermissionCheckerIdentityDecorator(identity, attributes.get(ROUTING_CONTEXT_ATTRIBUTE), permissionChecker));
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        return augment(identity, context, Map.of());
    }

    @Override
    public int priority() {
        // we do not rely on this value and always add this augmentor as the last one manually
        return Integer.MAX_VALUE;
    }

    private static final class PermissionCheckerIdentityDecorator implements SecurityIdentity {

        private final SecurityIdentity delegate;
        private final Object routingContext;
        private final BiFunction<SecurityIdentity, Permission, Uni<Boolean>> permissionChecker;

        private PermissionCheckerIdentityDecorator(SecurityIdentity delegate, Object routingContext,
                BiFunction<SecurityIdentity, Permission, Uni<Boolean>> permissionChecker) {
            this.delegate = delegate;
            this.routingContext = routingContext;
            this.permissionChecker = permissionChecker;
        }

        @Override
        public Principal getPrincipal() {
            return delegate.getPrincipal();
        }

        @Override
        public <T extends Principal> T getPrincipal(Class<T> clazz) {
            return delegate.getPrincipal(clazz);
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public Set<String> getRoles() {
            return delegate.getRoles();
        }

        @Override
        public boolean hasRole(String s) {
            return delegate.hasRole(s);
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> aClass) {
            return delegate.getCredential(aClass);
        }

        @Override
        public Set<Credential> getCredentials() {
            return delegate.getCredentials();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getAttribute(String s) {
            if (ROUTING_CONTEXT_ATTRIBUTE.equals(s)) {
                return (T) routingContext;
            }
            return delegate.getAttribute(s);
        }

        @Override
        public Map<String, Object> getAttributes() {
            if (routingContext != null) {
                Map<String, Object> attributes = new HashMap<>(delegate.getAttributes());
                attributes.put(ROUTING_CONTEXT_ATTRIBUTE, routingContext);
                return attributes;
            }
            return delegate.getAttributes();
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            return permissionChecker.apply(this, permission)
                    .flatMap(new Function<Boolean, Uni<? extends Boolean>>() {
                        @Override
                        public Uni<? extends Boolean> apply(Boolean accessGranted) {
                            if (Boolean.TRUE.equals(accessGranted)) {
                                return Uni.createFrom().item(true);
                            }
                            return delegate.checkPermission(permission);
                        }
                    });
        }

        @Override
        public boolean checkPermissionBlocking(Permission permission) {
            return checkPermission(permission).await().indefinitely();
        }
    }
}
