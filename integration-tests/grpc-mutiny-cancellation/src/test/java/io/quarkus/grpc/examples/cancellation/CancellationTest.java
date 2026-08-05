package io.quarkus.grpc.examples.cancellation;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.google.protobuf.Empty;

import examples.Item;
import examples.TestService;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.examples.cancellation.helper.CancellationTestRunner;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CancellationTest {
    @GrpcClient
    TestService testService;

    private CancellationTestRunner testRunner;

    private static final Consumer<Item> NOOP = item -> {
    };

    @BeforeEach
    void setUp() {
        testRunner = new CancellationTestRunner(testService);
    }

    @AfterEach
    void tearDown() {
        testRunner = null;
    }

    @Test
    @Order(1)
    void oneToOne() {
        Log.info("Starting oneToOne request...");
        testRunner.start(() -> testService.oneToOne(Empty.getDefaultInstance())
                .onCancellation().invoke(() -> Log.info("oneToOne cancelled"))
                .subscribe().with(NOOP));
        testRunner.awaitTermination();
    }

    @Test
    @Order(2)
    void oneToMany() {
        Log.info("Starting oneToMany request...");
        testRunner.start(() -> testService.oneToMany(Empty.getDefaultInstance())
                .onCancellation().invoke(() -> Log.info("oneToMany cancelled"))
                .subscribe().with(NOOP));
        testRunner.awaitTermination();
    }

    @Test
    @Order(3)
    void manyToOne() {
        Log.info("Starting manyToOne request...");
        Multi<Item> request = createMultiRequest()
                .onCancellation().invoke(() -> Log.info("manyToOne request cancelled"));

        testRunner.start(() -> testService.manyToOne(request)
                .onCancellation().invoke(() -> Log.info("manyToOne cancelled"))
                .subscribe().with(NOOP));
        testRunner.awaitTermination();
    }

    @Test
    @Order(4)
    void manyToMany() {
        Log.info("Starting manyToMany request...");
        Multi<Item> request = createMultiRequest()
                .onCancellation().invoke(() -> Log.info("manyToMany request cancelled"));

        testRunner.start(() -> testService.manyToMany(request)
                .onCancellation().invoke(() -> Log.info("manyToMany cancelled"))
                .subscribe().with(NOOP));
        testRunner.awaitTermination();
    }

    private Multi<Item> createMultiRequest() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(500))
                .select().first(10)
                .map(Long::intValue)
                .map(i -> Item.newBuilder().setValue(i).build());
    }
}
