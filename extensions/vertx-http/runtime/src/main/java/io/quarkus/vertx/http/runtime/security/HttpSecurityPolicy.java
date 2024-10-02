package io.quarkus.vertx.http.runtime.security;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * An HTTP Security policy, that controls which requests are allowed to proceed.
 * CDI beans implementing this interface are invoked on every request unless they define {@link #name()}.
 * The policy with {@link #name()} can then be referenced in the application.properties path matching rules,
 * or from the {@link AuthorizationPolicy#name()} annotation attribute.
 */
public interface HttpSecurityPolicy {

    Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext);

    /**
     * If HTTP Security policy name is not null, then this policy is only called in two cases:
     * - winning path-matching policy references this name in the application.properties
     * - invoked Jakarta REST endpoint references this name in the {@link AuthorizationPolicy#name()} annotation attribute
     * <p>
     * When the name is null, this policy is considered global and is applied on every single request.
     * More details and examples can be found in Quarkus documentation.
     *
     * @return policy name
     */
    default String name() {
        // null == global policy
        return null;
    }

    /**
     * The results of a permission check
     */
    class CheckResult {

        public static CheckResult DENY = new CheckResult(false);
        public static CheckResult PERMIT = new CheckResult(true);

        /**
         * If this check was successful
         */
        private final boolean permitted;

        /**
         * The new security identity, this allows the policy to add additional context
         * information to the identity. If this is null no change is made
         */
        private final SecurityIdentity augmentedIdentity;

        public CheckResult(boolean permitted) {
            this.permitted = permitted;
            this.augmentedIdentity = null;
        }

        public CheckResult(boolean permitted, SecurityIdentity augmentedIdentity) {
            this.permitted = permitted;
            this.augmentedIdentity = augmentedIdentity;
        }

        public boolean isPermitted() {
            return permitted;
        }

        public SecurityIdentity getAugmentedIdentity() {
            return augmentedIdentity;
        }

        public Uni<SecurityIdentity> getAugmentedIdentityAsUni() {
            return Uni.createFrom().item(augmentedIdentity);
        }

        public static Uni<CheckResult> permit() {
            return Uni.createFrom().item(PERMIT);
        }

        public static Uni<CheckResult> deny() {
            return Uni.createFrom().item(DENY);
        }
    }

    /**
     * A context object that can be used to run blocking tasks
     * <p>
     * Blocking identity providers should use this context object to run blocking tasks, to prevent excessive and
     * unnecessary delegation to thread pools
     */
    interface AuthorizationRequestContext {

        Uni<CheckResult> runBlocking(RoutingContext context, Uni<SecurityIdentity> identity,
                BiFunction<RoutingContext, SecurityIdentity, CheckResult> function);

    }

    class DefaultAuthorizationRequestContext implements AuthorizationRequestContext {
        private final BlockingSecurityExecutor blockingExecutor;

        DefaultAuthorizationRequestContext(BlockingSecurityExecutor blockingExecutor) {
            this.blockingExecutor = blockingExecutor;
        }

        @Override
        public Uni<HttpSecurityPolicy.CheckResult> runBlocking(RoutingContext context, Uni<SecurityIdentity> identityUni,
                BiFunction<RoutingContext, SecurityIdentity, HttpSecurityPolicy.CheckResult> function) {
            return identityUni
                    .flatMap(new Function<SecurityIdentity, Uni<? extends CheckResult>>() {
                        @Override
                        public Uni<? extends HttpSecurityPolicy.CheckResult> apply(SecurityIdentity identity) {
                            return blockingExecutor.executeBlocking(new Supplier<CheckResult>() {
                                @Override
                                public HttpSecurityPolicy.CheckResult get() {
                                    return function.apply(context, identity);
                                }
                            });
                        }
                    });
        }
    }
}
