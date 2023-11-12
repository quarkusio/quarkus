package io.quarkus.vertx.http.runtime.security;

import java.util.function.BiFunction;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * An HTTP Security policy, that controls which requests are allowed to proceed.
 * CDI beans implementing this interface are invoked on every request unless they define {@link #name()}.
 * The policy with {@link #name()} can then be referenced in the application.properties path matching rules,
 * which allows this policy to be applied only to specific requests.
 */
public interface HttpSecurityPolicy {

    Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext);

    /**
     * HTTP Security policy name referenced in the application.properties path matching rules, which allows this
     * policy to be applied to specific requests. The name must not be blank. When the name is {@code null}, policy
     * will be applied to every request.
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

}
