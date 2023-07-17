package io.quarkus.resteasy.test;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/length")
public class ContentLengthResource {

    @POST
    @Path("cl")
    public Response handleWithCl(String data) {
        byte[] body = data.getBytes(StandardCharsets.UTF_8);
        return Response.ok(body, "image/png").header("Content-Length", body.length).build();
    }

    @POST
    public Response handle(String data) {
        byte[] body = data.getBytes(StandardCharsets.UTF_8);
        return Response.ok(body, "image/png").build();
    }
}
