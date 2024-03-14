package io.quarkus.resteasy.reactive.server.test;

import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class RequestLeakDetectionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyRestAPI.class, MyRequestScopeBean.class, Barrier.class, Task.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    Barrier barrier;

    @Test
    public void testWithConcurrentCalls() {
        List<String> results = new CopyOnWriteArrayList<>();
        int count = 100;
        barrier.setMaxConcurrency(count);
        for (int i = 0; i < count; i++) {
            int c = i;
            new Thread(() -> {
                ResponseBody<?> body = RestAssured.given().pathParam("val", c).contentType(MediaType.APPLICATION_JSON)
                        .get("/test/{val}").thenReturn().body();
                String value = body.toString();
                results.add(value);
            }).start();
        }
        await().until(() -> results.size() == count);
        Set<String> asSet = new HashSet<>(results);
        Assertions.assertEquals(asSet.size(), count);
    }

    @Test
    public void testWithConcurrentBlockingCalls() {
        List<String> results = new CopyOnWriteArrayList<>();
        int count = 100;
        barrier.setMaxConcurrency(count);
        for (int i = 0; i < count; i++) {
            int c = i;
            new Thread(() -> {
                ResponseBody<?> body = RestAssured.given().pathParam("val", c).contentType(MediaType.APPLICATION_JSON)
                        .get("/test/blocking/{val}").thenReturn().body();
                String value = body.toString();
                results.add(value);
            }).start();
        }
        await().until(() -> results.size() == count);
        Set<String> asSet = new HashSet<>(results);
        Assertions.assertEquals(asSet.size(), count);
    }

    @ApplicationScoped
    @Path("/test")
    public static class MyRestAPI {

        @Inject
        MyRequestScopeBean bean;

        @Inject
        Barrier barrier;

        @GET
        @Path("/{val}")
        public Uni<Foo> foo(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int r = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(r, val);
                    e.complete(bean.getValue());
                });
            }).map(i -> new Foo(Integer.toString(i)));
        }

        @GET
        @Path("/blocking/{val}")
        public Foo blocking(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int r = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(r, val);
                    e.complete(bean.getValue());
                });
            })
                    .map(i -> new Foo(Integer.toString(i)))
                    .await().indefinitely();
        }
    }

    @ApplicationScoped
    public static class Barrier {

        private int level;

        public void setMaxConcurrency(int level) {
            this.level = level;
        }

        private final AtomicInteger counter = new AtomicInteger();
        private final List<Task> tasks = new CopyOnWriteArrayList<>();

        public void enqueue(Context context, Runnable runnable) {
            Task task = new Task(context, runnable);
            tasks.add(task);
            if (counter.incrementAndGet() >= level) {
                for (Task tbr : new ArrayList<>(tasks)) {
                    tbr.run();
                    tasks.remove(tbr);
                }
            }
        }
    }

    private static class Task {
        private final Context context;
        private final Runnable runnable;

        private Task(Context context, Runnable runnable) {
            this.context = context;
            this.runnable = runnable;
        }

        void run() {
            context.runOnContext(x -> runnable.run());
        }
    }

    @RequestScoped
    public static class MyRequestScopeBean {

        private int value = -1;

        public void setValue(int v) {
            if (value != -1) {
                throw new IllegalStateException("Already initialized");
            }
            value = v;
        }

        public int getValue() {
            return value;
        }

    }

    public static class Foo {

        public final String value;

        public Foo(String value) {
            this.value = value;
        }
    }

}
