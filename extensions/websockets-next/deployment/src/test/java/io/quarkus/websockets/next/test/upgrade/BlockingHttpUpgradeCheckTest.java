package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.VertxContextSupport;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class BlockingHttpUpgradeCheckTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(BlockingHttpUpgradeCheck.class, Endpoint.class, WSClient.class));

    @TestHTTPResource("/end")
    URI endUri;

    @Inject
    Vertx vertx;

    @Test
    public void testBlockingCheck() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(endUri);
            client.waitForMessages(1);
            assertEquals("ok", client.getMessages().get(0).toString());
            assertTrue(BlockingHttpUpgradeCheck.PERFORMED.get());
        }
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnOpen
        String open() {
            return "ok";
        }

    }

    @Singleton
    public static class BlockingHttpUpgradeCheck implements HttpUpgradeCheck {

        static final AtomicBoolean PERFORMED = new AtomicBoolean();

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            return VertxContextSupport.executeBlocking(new Callable<CheckResult>() {

                @Override
                public CheckResult call() throws Exception {
                    assertTrue(BlockingOperationControl.isBlockingAllowed());
                    assertTrue(VertxContext.isOnDuplicatedContext());
                    assertTrue(Arc.container().requestContext().isActive());
                    PERFORMED.set(true);
                    return CheckResult.permitUpgradeSync();
                }
            });
        }
    }
}
