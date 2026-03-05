package io.quarkus.vertx.http.management;

import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;

public class ManagementHostValidationWithoutHostTest {

    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.management.root-path=/management
            quarkus.management.host-validation.allowed-hosts=localhost
            """;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addClasses(MyObserver.class));

    @TestHTTPResource(value = "/my-route", management = true)
    URL uri;

    @Test
    public void requestWithoutHost() throws Exception {
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write("GET /management/my-route HTTP/1.0\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(IoUtil.readBytes(s.getInputStream()), StandardCharsets.UTF_8);
            Assertions.assertTrue(result.contains("403 Forbidden\r\n"),
                    "Expected 403 Forbidden but got: " + result);
            Assertions.assertFalse(result.contains("test-route"),
                    "Response must not contain 'test-route' but got: " + result);
        }
    }

    @Singleton
    static class MyObserver {

        public void registerManagementRoutes(@Observes ManagementInterface mi) {
            mi.router().get("/management/my-route").handler(rc -> rc.response().end("test route"));
        }

    }
}
