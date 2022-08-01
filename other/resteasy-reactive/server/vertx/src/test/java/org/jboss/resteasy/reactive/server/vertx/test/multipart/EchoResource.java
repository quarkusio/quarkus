package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import io.smallrye.common.annotation.NonBlocking;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
