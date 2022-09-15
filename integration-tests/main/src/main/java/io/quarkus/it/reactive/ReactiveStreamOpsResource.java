package io.quarkus.it.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.smallrye.mutiny.Multi;

@Path("/reactive")
public class ReactiveStreamOpsResource {

    @GET
    public String foo() {
        return "hello";
    }

    @GET
    @Path("/stream-regular")
    public String stream1() {
        StringBuilder builder = new StringBuilder();
        ReactiveStreams.of("a", "b", "c")
                .map(String::toUpperCase)
                .forEach(builder::append)
                .run();
        return builder.toString();
    }

    @GET
    @Path("/stream-mutiny")
    public String stream2() {
        StringBuilder builder = new StringBuilder();
        ReactiveStreams.fromPublisher(Multi.createFrom().items("d", "e", "f"))
                .map(String::toUpperCase)
                .forEach(builder::append)
                .run();
        return builder.toString();
    }

}
