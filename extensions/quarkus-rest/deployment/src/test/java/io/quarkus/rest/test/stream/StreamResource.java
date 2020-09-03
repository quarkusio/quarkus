package io.quarkus.rest.test.stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.rest.api.Stream;
import io.smallrye.mutiny.Multi;

@Path("stream")
public class StreamResource {

    @Path("collect")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> getCollectedText() {
        return Multi.createFrom().items("foo", "bar");
    }

    @Path("stream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Stream
    public Multi<String> getStreamedText() {
        return Multi.createFrom().items("foo", "bar");
    }
}
