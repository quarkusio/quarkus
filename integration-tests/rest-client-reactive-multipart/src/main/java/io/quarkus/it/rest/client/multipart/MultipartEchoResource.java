package io.quarkus.it.rest.client.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

/**
 * Must not be registered for reflection, so the native test checks that a multipart POJO response works without it.
 * Do not add methods with {@code File}, {@code SseEventSink} or {@code Multi} parameters or return types, or
 * multipart methods returning {@code Response} or {@code List}: any of those registers the whole class.
 */
@Path("/multipart-echo")
public class MultipartEchoResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public EchoBody echo(EchoBody body) {
        return body;
    }

    public static class EchoBody {
        @RestForm
        @PartType(MediaType.TEXT_PLAIN)
        public String text;
    }
}
