package io.quarkus.websockets.next.test.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

/**
 * Validate that the {@link AuthorizationPolicy#name()} must have
 * matching {@link io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy} bean.
 */
class HttpUpgradeMissingAuthorizationPolicyValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MissingPolicyEndpoint.class))
            .assertException(t -> assertThat(t.getMessage())
                    .contains("AuthorizationPolicy")
                    .contains("policy 'custom' is required")
                    .contains("MissingPolicyEndpoint"));

    @Test
    void runValidationTest() {
        // must be here in order to run validation
    }

    @AuthorizationPolicy(name = "custom")
    @WebSocket(path = "/validate-missing-policy")
    static class MissingPolicyEndpoint {

        @OnTextMessage
        void echo(String message) {
            // ignored
        }

    }
}
