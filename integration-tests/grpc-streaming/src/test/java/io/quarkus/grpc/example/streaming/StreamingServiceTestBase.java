package io.quarkus.grpc.example.streaming;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Channel;
import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.grpc.examples.streaming.StreamingGrpc;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class StreamingServiceTestBase {

    protected static final Duration TIMEOUT = Duration.ofSeconds(5);

    private Vertx _vertx;
    private Channel channel;

    protected Vertx vertx() {
        return null;
    }

    protected void close(Vertx vertx) {
    }

    @BeforeEach
    public void init() {
        _vertx = vertx();
        channel = GRPCTestUtils.channel(_vertx);
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
        close(_vertx);
    }

    @Test
    public void testSourceWithBlockingStub() {
        Iterator<Item> iterator = StreamingGrpc.newBlockingStub(channel).source(Empty.newBuilder().build());
        List<String> list = new ArrayList<>();
        iterator.forEachRemaining(i -> list.add(i.getValue()));
        assertThat(list).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testSourceWithMutinyStub() {
        Multi<Item> source = MutinyStreamingGrpc.newMutinyStub(channel).source(Empty.newBuilder().build());
        List<String> list = source.map(Item::getValue).collect().asList().await().atMost(TIMEOUT);
        assertThat(list).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testSinkWithMutinyStub() {
        Uni<Empty> done = MutinyStreamingGrpc.newMutinyStub(channel)
                .sink(Multi.createFrom().ticks().every(Duration.ofMillis(2))
                        .select().first(5)
                        .map(l -> Item.newBuilder().setValue(l.toString()).build()));
        done.await().atMost(TIMEOUT);
    }

    @Test
    public void testPipeWithMutinyStub() {
        Multi<Item> source = Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .select().first(5)
                .map(l -> Item.newBuilder().setValue(l.toString()).build());
        Multi<Item> results = MutinyStreamingGrpc.newMutinyStub(channel).pipe(source);

        List<Long> items = results
                .map(i -> Long.parseLong(i.getValue()))
                .collect().asList().await().atMost(TIMEOUT);

        // Resulting stream is: initial state (0), 0 + 0, 0 + 1, 1 + 2, 3 + 3, 6 + 4
        assertThat(items).containsExactly(0L, 0L, 1L, 3L, 6L, 10L);

    }

}
