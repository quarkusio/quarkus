package io.quarkus.resteasy.test;

import java.nio.charset.StandardCharsets;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

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
