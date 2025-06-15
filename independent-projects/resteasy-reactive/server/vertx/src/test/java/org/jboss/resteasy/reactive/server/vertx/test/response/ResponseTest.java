package org.jboss.resteasy.reactive.server.vertx.test.response;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseTest {

    @Test
    public void testCaseInsensitivity() {
        Response response = Response.status(Response.Status.METHOD_NOT_ALLOWED).header("allow", "HEAD")
                .header(HttpHeaders.HOST, "whatever").build();

        Assertions.assertEquals("HEAD", response.getHeaders().getFirst("allow"));
        Assertions.assertEquals("HEAD", response.getHeaders().getFirst(HttpHeaders.ALLOW));
    }

    @Test
    public void testLocation() {
        final var location = UriBuilder.fromUri("http://localhost:8080").path("{language}").build("en/us");
        Response response = Response.ok("Hello").location(location).build();
        Assertions.assertEquals("http://localhost:8080/en%2Fus", response.getLocation().toString());
    }

    @Test
    public void testContentLocation() {
        final var location = UriBuilder.fromUri("http://localhost:8080").path("{language}").build("en/us");
        Response response = Response.ok("Hello").contentLocation(location).build();
        Assertions.assertEquals("http://localhost:8080/en%2Fus", response.getHeaderString("Content-Location"));
    }
}
