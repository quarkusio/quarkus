package io.quarkus.vertx.http;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationWithoutHostTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.host-validation.allowed-hosts=localhost
                            """), "application.properties"));

    @TestHTTPResource
    URI uri;

    @Test
    public void requestWithoutHost() throws Exception {
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write("GET /test HTTP/1.0\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(IoUtil.readBytes(s.getInputStream()), StandardCharsets.UTF_8);
            Assertions.assertTrue(result.contains("403 Forbidden\r\n"),
                    "Expected 403 Forbidden but got: " + result);
            Assertions.assertFalse(result.contains("test-route"),
                    "Response must not contain 'test-route' but got: " + result);
        }
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");
            router.get("/test").handler(handler);
        }
    }
}
