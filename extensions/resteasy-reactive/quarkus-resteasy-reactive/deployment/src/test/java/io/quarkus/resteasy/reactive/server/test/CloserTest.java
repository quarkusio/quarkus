package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.Closer;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class CloserTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(PerRequestResource.class, SingletonResource.class, CounterResource.class,
                                    Counter.class);
                }
            }).overrideRuntimeConfigKey("quarkus.thread-pool.max-threads", "1")
            .overrideRuntimeConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    @Test
    public void test() {
        get("/counter/singleton")
                .then()
                .body(equalTo("0"));
        get("/counter/uni-singleton")
                .then()
                .body(equalTo("0"));
        get("/counter/per-request")
                .then()
                .body(equalTo("0"));

        get("/singleton")
                .then()
                .statusCode(200)
                .body(equalTo("0"));
        get("/singleton")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            get("/counter/singleton")
                    .then()
                    .body(equalTo("2"));
        });
        get("/counter/uni-singleton")
                .then()
                .body(equalTo("0"));
        get("/counter/per-request")
                .then()
                .body(equalTo("0"));

        get("/uni-singleton")
                .then()
                .statusCode(200)
                .body(equalTo("0"));
        get("/uni-singleton")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
        get("/counter/singleton")
                .then()
                .body(equalTo("2"));
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            get("/counter/uni-singleton")
                    .then()
                    .body(equalTo("2"));
        });
        get("/counter/per-request")
                .then()
                .body(equalTo("0"));

        get("/per-request")
                .then()
                .statusCode(200)
                .body(equalTo("0"));
        get("/per-request")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
        get("/counter/singleton")
                .then()
                .body(equalTo("2"));
        get("/counter/uni-singleton")
                .then()
                .body(equalTo("2"));
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            get("/counter/per-request")
                    .then()
                    .body(equalTo("2"));
        });
    }

    @Path("per-request")
    @RequestScoped
    public static class PerRequestResource implements Closeable {
        private final Closer closer;
        private final Counter counter;

        public PerRequestResource(Closer closer, Counter counter) {
            this.closer = closer;
            this.counter = counter;
        }

        @GET
        public int get() {
            closer.add(this);
            return counter.perRequest.get();
        }

        public void close() throws IOException {
            counter.perRequest.incrementAndGet();
        }
    }

    @Path("singleton")
    public static class SingletonResource implements Closeable {

        private final Counter counter;

        public SingletonResource(Counter counter) {
            this.counter = counter;
        }

        @GET
        public int get(@Context Closer closer) {
            closer.add(this);
            return counter.singleton.get();
        }

        @Override
        public void close() {
            counter.singleton.incrementAndGet();
        }
    }

    @Path("uni-singleton")
    public static class UniSingletonResource implements Closeable {

        @Inject
        Counter counter;

        @Inject
        Closer closer;

        public UniSingletonResource(Counter counter) {
            this.counter = counter;
        }

        @GET
        public Uni<Integer> get() {
            return Uni.createFrom().completionStage(() -> CompletableFuture.completedStage(null))
                    .invoke(() -> closer.add(UniSingletonResource.this))
                    .map(v -> counter.uniSingleton.get());
        }

        @Override
        public void close() {
            counter.uniSingleton.incrementAndGet();
        }
    }

    @Path("counter")
    public static class CounterResource {

        private final Counter counter;

        public CounterResource(Counter counter) {
            this.counter = counter;
        }

        @Path("singleton")
        @GET
        public int singletonCount() {
            return counter.singleton.get();
        }

        @Path("uni-singleton")
        @GET
        public int uniSingleton() {
            return counter.uniSingleton.get();
        }

        @Path("per-request")
        @GET
        public int perRequestCount() {
            return counter.perRequest.get();
        }
    }

    @Singleton
    public static class Counter {
        public final AtomicInteger perRequest = new AtomicInteger(0);
        public final AtomicInteger singleton = new AtomicInteger(0);
        public final AtomicInteger uniSingleton = new AtomicInteger(0);

    }
}
