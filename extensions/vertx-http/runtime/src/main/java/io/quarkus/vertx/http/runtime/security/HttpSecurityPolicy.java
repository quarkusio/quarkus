package io.quarkus.vertx.http.runtime.security;

import java.util.function.BiFunction;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * A HTTP Security policy, that controls which requests are allowed to proceeed.
 *
 * There are two different ways these policies can be installed. The easiest is to just create a CDI bean, in which
 * case the policy will be invoked on every request.
 *
 * Alternatively HttpSecurityPolicyBuildItem can be used to create a named policy. This policy can then be referenced
 * in the application.properties path matching rules, which allows this policy to be applied to specific requests.
 */
public interface HttpSecurityPolicy {

    Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext);

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
     * Blocking identity providers should used this context object to run blocking tasks, to prevent excessive and
     * unnecessary delegation to thread pools
     */
    interface AuthorizationRequestContext {

        Uni<CheckResult> runBlocking(RoutingContext context, Uni<SecurityIdentity> identity,
                BiFunction<RoutingContext, SecurityIdentity, CheckResult> function);

    }

}
