package io.quarkus.websockets.next.test.upgrade;

import static io.quarkus.websockets.next.test.upgrade.GlobalHttpUpgradeCheckTest.ChainHttpUpgradeCheckBase.TEST_CHECK_CHAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.Router;

public class GlobalHttpUpgradeCheckTest extends AbstractHttpUpgradeCheckTestBase {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Opening.class, Responding.class, OpeningHttpUpgradeCheck.class,
                            RejectingHttpUpgradeCheck.class, WSClient.class, OpeningHttpUpgradeCheckBean.class,
                            RejectingHttpUpgradeCheckBean.class, ChainHttpUpgradeCheckBase.class,
                            ChainHttpUpgradeCheck4.class, ChainHttpUpgradeCheck3.class, ChainHttpUpgradeCheck2.class,
                            ChainHttpUpgradeCheck1.class, NullCheckResultCheck.class, Rejecting.class,
                            ResponseHeadersObserver.class));

    @Test
    public void testNullCheckResultNotAllowed() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(
                            new WebSocketConnectOptions().addHeader(NullCheckResultCheck.NULL_CHECK, "ignored"),
                            openingUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("500"), root.getMessage());
        }
    }

    @Test
    public void testHttpUpgradeChecksOrdered() {
        ChainHttpUpgradeCheckBase.INVOCATION_COUNT.set(0);
        ResponseHeadersObserver.responseHeaders = null;

        // expect the checks are ordered by @Priority
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(
                            new WebSocketConnectOptions().addHeader(TEST_CHECK_CHAIN, "ignored"),
                            openingUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());

            Awaitility.await()
                    .atMost(Duration.ofSeconds(2))
                    .until(() -> ResponseHeadersObserver.responseHeaders != null
                            && !ResponseHeadersObserver.responseHeaders.isEmpty());
            var headers = ResponseHeadersObserver.responseHeaders;
            var orderedPriorities = headers
                    .entries()
                    .stream()
                    .filter(e -> "1".equals(e.getKey()) || "2".equals(e.getKey()) || "3".equals(e.getKey())
                            || "4".equals(e.getKey()))
                    .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getKey())))
                    .map(Map.Entry::getValue)
                    .map(Integer::parseInt)
                    .toList();
            assertEquals(4, orderedPriorities.size());

            int prev = 1000000;
            for (int next : orderedPriorities) {
                if (prev <= next) {
                    Assertions.fail("HttpUpgradeChecks are not ordered: " + orderedPriorities);
                }
                prev = next;
            }
        }
    }

    @Singleton
    public static class OpeningHttpUpgradeCheckBean extends OpeningHttpUpgradeCheck {

    }

    @ApplicationScoped
    public static class RejectingHttpUpgradeCheckBean extends RejectingHttpUpgradeCheck {

    }

    public static abstract class ChainHttpUpgradeCheckBase implements HttpUpgradeCheck {

        static final String TEST_CHECK_CHAIN = "test-check-chain";
        static final AtomicInteger INVOCATION_COUNT = new AtomicInteger(0);

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext request) {
            return request.securityIdentity().chain(identity -> {
                if (identity != null && identity.isAnonymous() && testCheckChain(request)) {
                    return CheckResult.permitUpgrade(getResponseHeaders());
                }
                return CheckResult.permitUpgrade();
            });
        }

        protected Map<String, List<String>> getResponseHeaders() {
            return Map.of(Integer.toString(INVOCATION_COUNT.incrementAndGet()), List.of(Integer.toString(priority())));
        }

        protected abstract int priority();

        protected static boolean testCheckChain(HttpUpgradeContext context) {
            return context.httpRequest().headers().contains(TEST_CHECK_CHAIN);
        }

    }

    @Dependent
    public static final class ChainHttpUpgradeCheck1 extends ChainHttpUpgradeCheckBase {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext request) {
            if (testCheckChain(request)) {
                return CheckResult.rejectUpgrade(401, getResponseHeaders());
            }
            return super.perform(request);
        }

        @Override
        protected int priority() {
            // default priority
            return 0;
        }
    }

    @Priority(10)
    @Dependent
    public static final class ChainHttpUpgradeCheck2 extends ChainHttpUpgradeCheckBase {

        @Override
        protected int priority() {
            return 10;
        }
    }

    @Priority(100)
    @Dependent
    public static final class ChainHttpUpgradeCheck3 extends ChainHttpUpgradeCheckBase {

        @Override
        protected int priority() {
            return 100;
        }
    }

    @Priority(1000)
    @Dependent
    public static final class ChainHttpUpgradeCheck4 extends ChainHttpUpgradeCheckBase {

        @Override
        protected int priority() {
            return 1000;
        }
    }

    @Dependent
    public static final class NullCheckResultCheck implements HttpUpgradeCheck {

        static final String NULL_CHECK = "null-check";

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            if (context.httpRequest().headers().contains(NULL_CHECK)) {
                return Uni.createFrom().nullItem();
            }
            return CheckResult.permitUpgrade();
        }
    }

    public static final class ResponseHeadersObserver {

        static volatile MultiMap responseHeaders = null;

        void observer(@Observes Router router) {
            router.route().order(0).handler(ctx -> {
                ctx.addHeadersEndHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void unused) {
                        responseHeaders = ctx.response().headers();
                    }
                });
                ctx.next();
            });
        }

    }
}
