package io.quarkus.grpc.example.streaming;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.grpc.examples.streaming.StringReply;
import io.grpc.examples.streaming.StringRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class StreamingService extends MutinyStreamingGrpc.StreamingImplBase {

    @Override
    public Multi<Item> source(Empty request) {
        return Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .select().first(10)
                .map(l -> Item.newBuilder().setValue(Long.toString(l)).build());
    }

    @Override
    public Uni<Empty> sink(Multi<Item> request) {
        return request
                .map(Item::getValue)
                .map(Long::parseLong)
                .collect().last()
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

    @Override
    public Uni<StringReply> quickStringStream(Multi<StringRequest> request) {
        return request
                .call(() -> {
                    throw new RuntimeException("Any error");
                })
                .map(x -> StringReply.newBuilder().setMessage(x.toString()).build())
                .collect().asList()
                .replaceWith(StringReply.newBuilder().setMessage("DONE").build());
    }

    @Override
    public Uni<StringReply> midStringStream(Multi<StringRequest> request) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        return request
                .map(x -> {
                    if (atomicInteger.getAndIncrement() == 3) {
                        throw new RuntimeException("We reached 3, error here");
                    }
                    return StringReply.newBuilder().setMessage(x.toString()).build();
                })
                .collect().asList()
                .replaceWith(StringReply.newBuilder().setMessage("DONE").build());
    }

    @Override
    public Multi<StringReply> quickStringBiDiStream(Multi<StringRequest> request) {
        return request
                .call(() -> {
                    throw new RuntimeException("Any error");
                })
                .map(x -> StringReply.newBuilder().setMessage(x.toString()).build());
    }

    @Override
    public Multi<StringReply> midStringBiDiStream(Multi<StringRequest> request) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        return request
                .map(x -> {
                    if (atomicInteger.getAndIncrement() == 3) {
                        throw new RuntimeException("We reached 3, error here");
                    }
                    return StringReply.newBuilder().setMessage(x.toString()).build();
                });
    }
}
