package io.quarkus.grpc.example.streaming;

import java.time.Duration;

import javax.inject.Singleton;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Singleton
public class StreamingService extends MutinyStreamingGrpc.StreamingImplBase {

    @Override
    public Multi<Item> source(Empty request) {
        return Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .transform().byTakingFirstItems(10)
                .map(l -> Item.newBuilder().setValue(Long.toString(l)).build());
    }

    @Override
    public Uni<Empty> sink(Multi<Item> request) {
        return request
                .map(Item::getValue)
                .map(Long::parseLong)
                .collectItems().last()
                .map(l -> Empty.newBuilder().build());
    }

    @Override
    public Multi<Item> pipe(Multi<Item> request) {
        return request
                .map(Item::getValue)
                .map(Long::parseLong)
                .onItem().scan(() -> 0L, Long::sum)
                .onItem().transform(l -> Item.newBuilder().setValue(Long.toString(l)).build());
    }
}
