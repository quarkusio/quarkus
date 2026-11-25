package io.quarkus.it.opentelemetry.reactive;

import java.io.File;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

@RegisterRestClient(configKey = "client")
@Path("/parent-span")
public interface ParentSpanRestClient {

    @POST
    @Path("/string")
    String string(String body);

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    byte[] stream(InputStream body);

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    byte[] file(File body);

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    byte[] multi(Multi<Buffer> body);

    @POST
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    String multipart(@RestForm String name, @RestForm File file);
}
