package io.quarkus.websockets.next.test.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Validate that the {@link AuthorizationPolicy} annotation on method is not allowed.
 */
class HttpUpgradeAuthorizationPolicyOnMethodValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PolicyOnMethodEndpoint.class, CustomPolicy.class))
            .assertException(t -> assertThat(t.getMessage())
                    .contains("AuthorizationPolicy")
                    .contains("methodAnnotatedWithPolicy"));

    @Test
    void runValidationTest() {
        // must be here in order to run validation
    }

    @ApplicationScoped
    static class CustomPolicy implements HttpSecurityPolicy {

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            return CheckResult.deny();
        }

        @Override
        public String name() {
            return "custom";
        }
    }

    @WebSocket(path = "/validate-policy-on-method")
    static class PolicyOnMethodEndpoint {

        @AuthorizationPolicy(name = "custom")
        @OnTextMessage
        void methodAnnotatedWithPolicy(String message) {
            // ignored
        }

    }
}
