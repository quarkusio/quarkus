package io.quarkus.resteasy.test;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class VertxIOHangTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(InputStreamResource.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void testDelayFilter() {
        // makes sure that everything works as normal
        RestAssured.given().body("hello world").post("/in").then().body(Matchers.is("hello world"));
    }

    @Test
    public void testDelayFilterConnectionKilled() throws Exception {
        // makes sure that everything works as normal
        try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
            s.getOutputStream().write(
                    "POST /in HTTP/1.1\r\nHost:localhost\r\nContent-Length: 100\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
        }
        Throwable exception = InputStreamResource.THROWABLES.poll(3, TimeUnit.SECONDS);
        Assertions.assertTrue(exception instanceof IOException);
    }
}
