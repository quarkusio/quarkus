package io.quarkus.websockets.next.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;

public class HttpUpgradeSelectMultipleAuthMechValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.proactive=false
                            """), "application.properties")
                    .addClasses(WSClient.class, MultipleAuthMechanismsEndpoint.class))
            .assertException(t -> {
                String message = t.getMessage();
                Assertions.assertNotNull(message);
                Assertions.assertTrue(message.contains("Only one of the"));
                Assertions.assertTrue(message.contains("BasicAuthentication"));
                Assertions.assertTrue(message.contains("FormAuthentication"));
                Assertions.assertTrue(message.contains("MultipleAuthMechanismsEndpoint"));
                Assertions.assertTrue(message.contains("annotations can be applied on the"));
                Assertions.assertTrue(message.contains("MultipleAuthMechanismsEndpoint"));
                Assertions.assertTrue(message.contains("class"));
            });

    @Test
    public void runValidationTest() {
        // must be here in order to run validation
    }

    @BasicAuthentication
    @FormAuthentication
    @WebSocket(path = "/validate-multiple-auth-mechanisms")
    public static class MultipleAuthMechanismsEndpoint {

        @OnTextMessage
        void onMessage(String message) {
            // ignored
        }

    }
}
