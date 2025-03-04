package io.quarkus.websockets.next.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;

public class HttpUpgradeSelectAuthMechOnMethodValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.proactive=false
                            """), "application.properties")
                    .addClasses(WSClient.class, BasicAuthOnMethodEndpoint.class))
            .assertException(t -> {
                String message = t.getMessage();
                Assertions.assertNotNull(message);
                Assertions.assertTrue(message.contains("BasicAuthentication"));
                Assertions.assertTrue(message.contains("cannot be applied on the"));
                Assertions.assertTrue(message.contains("BasicAuthOnMethodEndpoint"));
                Assertions.assertTrue(message.contains("onMessage"));
                Assertions.assertTrue(message.contains("please move the annotations to the class-level instead"));
            });

    @Test
    public void runValidationTest() {
        // must be here in order to run validation
    }

    @WebSocket(path = "/validate-basic-auth-on-method")
    public static class BasicAuthOnMethodEndpoint {

        @BasicAuthentication
        @OnTextMessage
        void onMessage(String message) {
            // ignored
        }

    }
}
