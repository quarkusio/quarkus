package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.UpgradeRejectedException;

public class HttpUpgradeCheckPathParamsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Endpoint.class, UpgradeCheck.class, WSClient.class));

    @Inject
    Vertx vertx;

    @TestHTTPResource("accept")
    URI acceptUri;

    @TestHTTPResource("reject")
    URI rejectUri;

    @BeforeEach
    public void cleanUp() {
        Endpoint.OPENED.set(false);
    }

    @Test
    public void testHttpUpgradeRejected() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(rejectUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("404"), root.getMessage());
        }
    }

    @Test
    public void testHttpUpgradePermitted() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(acceptUri);
            Awaitility.await().atMost(Duration.ofSeconds(2)).until(Endpoint.OPENED::get);
        }
    }

    @WebSocket(path = "/{action}")
    public static class Endpoint {

        static final AtomicBoolean OPENED = new AtomicBoolean();

        @OnOpen
        void onOpen() {
            OPENED.set(true);
        }

    }

    @Singleton
    public static class UpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            if ("reject".equals(context.pathParam("action"))) {
                return CheckResult.rejectUpgrade(404);
            }
            return CheckResult.permitUpgrade();
        }

    }
}
