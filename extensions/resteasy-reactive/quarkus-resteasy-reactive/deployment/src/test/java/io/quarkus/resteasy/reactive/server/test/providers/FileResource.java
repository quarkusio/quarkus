package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.File;
import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.FilePart;
import org.jboss.resteasy.reactive.PathPart;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.web.RoutingContext;

@Path("providers/file")
public class FileResource {

    private static final String FILE = "src/test/resources/lorem.txt";

    @Path("file")
    @GET
    public File getFile() {
        return new File(FILE);
    }

    @Path("file-partial")
    @GET
    public FilePart getFilePart() {
        return new FilePart(new File(FILE), 20, 10);
    }

    @Path("path")
    @GET
    public java.nio.file.Path getPath() {
        return Paths.get(FILE);
    }

    @Path("path-partial")
    @GET
    public PathPart getPathPart() {
        return new PathPart(Paths.get(FILE), 20, 10);
    }

    @Path("async-file")
    @GET
    public Uni<AsyncFile> getAsyncFile(RoutingContext vertxRequest) {
        return Uni.createFrom().emitter(emitter -> {
            vertxRequest.vertx().fileSystem().open(FILE, new OpenOptions(), result -> {
                if (result.succeeded())
                    emitter.complete(result.result());
                else
                    emitter.fail(result.cause());
            });
        });
    }

    @Path("mutiny-async-file")
    @GET
    public Uni<io.vertx.mutiny.core.file.AsyncFile> getMutinyAsyncFile(RoutingContext vertxRequest) {
        return new io.vertx.mutiny.core.Vertx(vertxRequest.vertx()).fileSystem().open(FILE, new OpenOptions());
    }

    @Path("async-file-partial")
    @GET
    public Uni<AsyncFile> getAsyncFilePartial(RoutingContext vertxRequest) {
        return Uni.createFrom().emitter(emitter -> {
            vertxRequest.vertx().fileSystem().open(FILE, new OpenOptions(), result -> {
                if (result.succeeded()) {
                    AsyncFile asyncFile = result.result();
                    asyncFile.setReadPos(20);
                    asyncFile.setReadLength(10);
                    emitter.complete(asyncFile);
                } else
                    emitter.fail(result.cause());
            });
        });
    }
}
