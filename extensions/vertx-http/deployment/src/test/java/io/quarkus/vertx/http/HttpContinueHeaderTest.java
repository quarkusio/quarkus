package io.quarkus.vertx.http;

import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpContinueHeaderTest {
    private static final String APP_PROPS = "quarkus.http.handle-100-continue-automatically=true\n";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(HttpContinueHeaderTest.BeanRegisteringRouteUsingObserves.class));

    @TestHTTPResource
    URL uri;

    @Test
    public void testConnection() throws Exception {
        try (Socket one = new Socket(uri.getHost(), uri.getPort())) {
            one.getOutputStream().write("GET /hello HTTP/1.1\r\nExpect: 100-continue\r\nHost: localhost\r\n\r\n"
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
            Assertions.assertTrue(sb.toString().contains("HTTP/1.1 100 Continue"));
            Assertions.assertTrue(sb.toString().contains("HTTP/1.1 200 OK"));
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
