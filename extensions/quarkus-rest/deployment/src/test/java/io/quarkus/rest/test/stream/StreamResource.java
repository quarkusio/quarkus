package io.quarkus.rest.test.stream;

import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("stream")
public class StreamResource {

    @Path("collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getCollectedText() {
        return Multi.createFrom().items("foo", "bar")
                .collectItems().with(Collectors.joining());
    }

    @Path("stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> getStreamedText() {
        return Multi.createFrom().items("foo", "bar");
    }
}
