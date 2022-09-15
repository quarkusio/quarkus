package org.jboss.resteasy.reactive.server.vertx.test.response;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseTest {

    @Test
    public void testCaseInsensitivity() {
        Response response = Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .header("allow", "HEAD").header(HttpHeaders.HOST, "whatever").build();

        Assertions.assertEquals("HEAD", response.getHeaders().getFirst("allow"));
        Assertions.assertEquals("HEAD", response.getHeaders().getFirst(HttpHeaders.ALLOW));
    }
}
