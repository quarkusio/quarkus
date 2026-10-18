package io.quarkus.grpc.examples.cancellation;

import java.time.Duration;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.Empty;

import examples.Item;
import examples.StatusResponse;
import examples.TestService;
import io.quarkus.grpc.GrpcService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;

@GrpcService
public class TestServiceImpl implements TestService {
    AtomicReference<MultiEmitter<? super StatusResponse>> emitter = new AtomicReference<>();

    @Override
    public Uni<Item> oneToOne(Empty request) {
        return Uni.createFrom().item(15)
                .onItem().delayIt().by(Duration.ofSeconds(3))
                .map(i -> Item.newBuilder().setValue(i).build())
                .onTermination().invoke(this::emitTermination)
                .onCancellation().invoke(this::emitCancellation)
                .onCancellation().invoke(() -> Log.info("oneToOne cancelled on server"))
                .onSubscription().invoke(this::emitSubscription);
    }

    @Override
    public Multi<Item> oneToMany(Empty request) {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .select().first(3)
                .map(Long::intValue)
                .map(i -> Item.newBuilder().setValue(i).build())
                .onTermination().invoke(this::emitTermination)
                .onCancellation().invoke(this::emitCancellation)
                .onCancellation().invoke(() -> Log.info("oneToMany cancelled on server"))
                .onSubscription().invoke(this::emitSubscription);
    }

    @Override
    public Uni<Item> manyToOne(Multi<Item> request) {
        return request
                .map(Item::getValue)
                .collect().last()
                .map(i -> Item.newBuilder().setValue(i).build())
                .onTermination().invoke(this::emitTermination)
                .onCancellation().invoke(this::emitCancellation)
                .onCancellation().invoke(() -> Log.info("manyToOne cancelled on server"))
                .onSubscription().invoke(this::emitSubscription);
    }

    @Override
    public Multi<Item> manyToMany(Multi<Item> request) {
        return request
                .map(Item::getValue)
                .onItem().scan(Integer::sum)
                .map(i -> Item.newBuilder().setValue(i).build())
                .onTermination().invoke(this::emitTermination)
                .onCancellation().invoke(this::emitCancellation)
                .onCancellation().invoke(() -> Log.info("manyToMany cancelled on server"))
                .onSubscription().invoke(this::emitSubscription);
    }

    @Override
    public Multi<StatusResponse> readStatus(Empty request) {
        return Multi.createFrom().emitter(e -> {
            emitter.set(e);
            emitReady();
        });
    }

    private void emitReady() {
        emitStatus(StatusResponse.Status.READY);
    }

    private void emitSubscription(Subscription subscription) {
        emitStatus(StatusResponse.Status.SUBSCRIBED);
    }

    private void emitCancellation() {
        emitStatus(StatusResponse.Status.CANCELLED);
    }

    private void emitTermination() {
        MultiEmitter<? super StatusResponse> e = emitter.get();
        if (e != null) {
            e.complete();
        }
    }

    private void emitStatus(StatusResponse.Status status) {
        MultiEmitter<? super StatusResponse> e = emitter.get();
        if (e != null) {
            e.emit(StatusResponse.newBuilder().setStatus(status).build());
        }
    }
}
