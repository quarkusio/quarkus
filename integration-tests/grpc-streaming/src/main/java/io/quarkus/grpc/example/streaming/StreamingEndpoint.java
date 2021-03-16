package io.quarkus.grpc.example.streaming;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/streaming")
public class StreamingEndpoint {

    @Inject
    @GrpcService("streaming")
    MutinyStreamingGrpc.MutinyStreamingStub client;

    @GET
    public Multi<String> invokeSource() {
        return client.source(Empty.newBuilder().build())
                .onItem().transform(Item::getValue);
    }

    @GET
    @Path("sink/{max}")
    public Uni<Void> invokeSink(@PathParam("max") int max) {
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return client.sink(inputs).onItem().ignore().andContinueWithNull();
    }

    @GET
    @Path("/{max}")
    public Multi<String> invokePipe(@PathParam("max") int max) {
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return client.pipe(inputs).onItem().transform(Item::getValue);
    }
}
