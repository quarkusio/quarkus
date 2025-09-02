package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.UserData;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class HttpUpgradeCheckUserDataTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Endpoint.class, UpgradeCheck.class, WSClient.class));

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endpointUri;

    @Test
    public void testHttpUpgradeCheckPassesUserData() throws InterruptedException {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(endpointUri);
            assertTrue(Endpoint.OPEN.await(2, TimeUnit.SECONDS));
            assertTrue(Endpoint.USER_DATA.get().get(UserData.TypedKey.forBoolean("boolean")));
            assertEquals(Integer.MAX_VALUE, Endpoint.USER_DATA.get().get(UserData.TypedKey.forInt("int")));
            assertEquals(Long.MAX_VALUE, Endpoint.USER_DATA.get().get(UserData.TypedKey.forLong("long")));
            assertEquals("Hello", Endpoint.USER_DATA.get().get(UserData.TypedKey.forString("string")));
        }
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final CountDownLatch OPEN = new CountDownLatch(1);
        static final AtomicReference<UserData> USER_DATA = new AtomicReference<>();

        @OnOpen
        void onOpen(WebSocketConnection connection) {
            USER_DATA.set(connection.userData());
            OPEN.countDown();
        }

    }

    @Singleton
    public static class UpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            context.userData().put(UserData.TypedKey.forBoolean("boolean"), true);
            context.userData().put(UserData.TypedKey.forInt("int"), Integer.MAX_VALUE);
            context.userData().put(UserData.TypedKey.forLong("long"), Long.MAX_VALUE);
            context.userData().put(UserData.TypedKey.forString("string"), "Hello");
            return CheckResult.permitUpgrade();
        }

    }
}
