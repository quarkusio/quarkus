package io.quarkus.grpc.example.streaming;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/streaming")
public class StreamingEndpoint {

    @GrpcClient
    MutinyStreamingGrpc.MutinyStreamingStub streaming;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> invokeSource() {
        return streaming.source(Empty.newBuilder().build())
                .onItem().transform(Item::getValue);
    }

    @GET
    @Path("sink/{max}")
    public Uni<Void> invokeSink(@PathParam("max") int max) {
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return streaming.sink(inputs).onItem().ignore().andContinueWithNull();
    }

    @GET
    @Path("/{max}")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> invokePipe(@PathParam("max") int max) {
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return streaming.pipe(inputs).onItem().transform(Item::getValue);
    }
}
