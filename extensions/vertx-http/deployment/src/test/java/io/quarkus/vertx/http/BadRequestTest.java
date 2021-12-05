package io.quarkus.vertx.http;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BadRequestTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    public void test() throws Exception {
        //we need to manually send a bad request
        //most clients will escape a bad request for you
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write("GET /foo?name=Iva%n HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            String result = new String(IoUtil.readBytes(s.getInputStream()), StandardCharsets.UTF_8);
            Assertions.assertTrue(result.contains("HTTP/1.1 400 Bad Request\r\n"));
        }
    }
}
