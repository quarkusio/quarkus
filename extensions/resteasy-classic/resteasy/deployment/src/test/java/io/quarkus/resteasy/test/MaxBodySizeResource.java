package io.quarkus.resteasy.test;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

@Path("/max-body-size")
public class MaxBodySizeResource {

    @POST
    @Produces("text/plain")
    @Consumes("text/plain")
    public String post(String body, @Context HttpHeaders headers) {
        return (headers.getHeaderString(HttpHeaders.CONTENT_LENGTH) == null ? "chunked" : "cl") + body;
    }

    @POST
    @Produces("text/plain")
    @Consumes("application/octet-stream")
    public String post(byte[] body, @Context HttpHeaders headers) {
        return (headers.getHeaderString(HttpHeaders.CONTENT_LENGTH) == null ? "chunked" : "cl")
                + new String(body, StandardCharsets.UTF_8);
    }
}
