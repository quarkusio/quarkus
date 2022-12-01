package io.quarkus.vertx.http;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ConnectionLimitsTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.limits.max-connections=1\n";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @TestHTTPResource
    URL uri;

    @Test
    public void testConnectionLimits() throws Exception {
        try (Socket one = new Socket(uri.getHost(), uri.getPort())) {
            one.getOutputStream().write("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            byte[] data = new byte[1024];
            int j;
            while (!sb.toString().endsWith("hello")) {
                j = one.getInputStream().read(data);
                if (j == -1) {
                    Assertions.fail("Did not read full HTTP response");
                }
                sb.append(new String(data, 0, j, StandardCharsets.US_ASCII));
            }
            //we now have one connection, and it has performed a request
            //start another one, it should fail

            try (Socket two = new Socket(uri.getHost(), uri.getPort())) {
                two.getOutputStream().write("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                int res = two.getInputStream().read(data);
                if (res > 0) {
                    Assertions.fail("Expected connection to fail");
                }
            } catch (IOException expected) {

            }
            //verify the first connection is still fine
            sb.setLength(0);
            one.getOutputStream().write("GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(one.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            Assertions.assertTrue(result.endsWith("hello"));
            Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(new ThrowingRunnable() {
                        @Override
                        public void run() throws Throwable {
                            //first connection is closed, try second connection
                            try (Socket two = new Socket(uri.getHost(), uri.getPort())) {
                                two.getOutputStream()
                                        .write("GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                                                .getBytes(StandardCharsets.UTF_8));
                                int res = two.getInputStream().read(data);
                                Assertions.assertTrue(res > 0);
                            }
                        }
                    });

        }
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.route("/hello").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.end("hello");
                }
            });

        }

    }

}
