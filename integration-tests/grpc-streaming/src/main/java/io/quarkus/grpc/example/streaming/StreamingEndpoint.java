package io.quarkus.grpc.example.streaming;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jboss.logging.Logger;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/streaming")
public class StreamingEndpoint {

    private static final Logger log = Logger.getLogger(StreamingEndpoint.class);

    @GrpcClient
    MutinyStreamingGrpc.MutinyStreamingStub streaming;

    @GET
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
    public Multi<String> invokePipe(@PathParam("max") int max) {
        //    public List<String> invokePipe(@PathParam("max") int max) {
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .invoke(value -> log.infof("the client is emitting %s", value))
                .map(i -> Item.newBuilder().setValue(i).build());
        //        List<String> collector = new CopyOnWriteArrayList<>();
        //        CompletableFuture<Void> finish = new CompletableFuture<>();
        return streaming.pipe(inputs)
                .onFailure().invoke(error -> log.error("gRPC failure", error))
                .onItem().transform(Item::getValue)
                .onItem().invoke(value -> log.infof("received %s", value))
                .onCompletion().invoke(() -> log.info("completed"));
        //                .subscribe().with(
        //                collector::add, () -> finish.complete(null));
        /*
         * try {
         * finish.get(10, TimeUnit.SECONDS);
         * } catch (InterruptedException | ExecutionException e) {
         * throw new IllegalStateException("Failed waiting for responses", e);
         * } catch (TimeoutException e) {
         * throw new IllegalStateException("Timed out waiting for responses", e);
         * }
         * return collector;
         */
    }
}
