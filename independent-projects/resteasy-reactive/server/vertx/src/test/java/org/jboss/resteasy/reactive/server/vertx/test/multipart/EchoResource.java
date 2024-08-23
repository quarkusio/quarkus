package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.NonBlocking;

@Path("/echo")
public class EchoResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @NonBlocking
    public String echo(byte[] request) {
        return new String(request, StandardCharsets.UTF_8);
    }
}
