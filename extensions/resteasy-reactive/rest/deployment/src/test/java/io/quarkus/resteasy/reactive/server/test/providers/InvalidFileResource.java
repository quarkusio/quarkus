package io.quarkus.resteasy.reactive.server.test.providers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.core.file.AsyncFile;
import io.vertx.ext.web.RoutingContext;

@Path("providers/file-invalid")
public class InvalidFileResource {

    @WithWriterInterceptor
    @Path("async-file-blocking")
    @GET
    public AsyncFile getAsyncFileBlocking(RoutingContext vertxRequest) {
        // we're not calling this anyway
        return null;
    }
}
