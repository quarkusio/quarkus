package io.quarkus.it.opentelemetry.reactive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;

@Path("/parent-span")
public class ParentSpanResource {

    @POST
    @Path("/string")
    public String string(String body) {
        return body;
    }

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] stream(InputStream body) throws IOException {
        return body.readAllBytes();
    }

    @POST
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String multipart(@RestForm String name, @RestForm File file) {
        return name;
    }
}
