package org.jboss.resteasy.reactive.server.vertx.test.providers;

import io.vertx.core.file.AsyncFile;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
