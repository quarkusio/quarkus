package io.quarkus.rest.client.reactive;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
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

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRestAPI.class, MyRequestScopeBean.class, Barrier.class, Task.class, RemoteClient.class,
                            RemoteService.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource(
                            new StringAsset(setUrlForClass(RemoteClient.class)), "application.properties"));

    @Inject
    Barrier barrier;

    @ParameterizedTest
    @ValueSource(strings = {
            "reactive-server-and-client",
            "blocking-server-and-reactive-client",
            "blocking-server-and-client",
            "reactive-server-and-blocking-client"
    })
    public void testWithConcurrentCallsWithReactiveClientAndServer(String path) {
        List<String> results = new CopyOnWriteArrayList<>();
        int count = 100;
        barrier.setMaxConcurrency(count);
        for (int i = 0; i < count; i++) {
            int c = i;
            new Thread(() -> {
                ResponseBody<?> body = RestAssured.given().pathParam("val", c).contentType(MediaType.APPLICATION_JSON)
                        .get("/test/" + path + "/{val}").thenReturn().body();
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

        @RestClient
        RemoteClient client;

        @GET
        @Path("/reactive-server-and-client/{val}")
        public Uni<Foo> reactiveServerAndClient(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    Assertions.assertEquals(bean.getValue(), val);
                    int rBefore = Vertx.currentContext().getLocal("count");
                    client.invokeReactive(Integer.toString(val))
                            .invoke(s -> {
                                Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                                int rAfter = Vertx.currentContext().getLocal("count");
                                Assertions.assertEquals(s, "hello " + rAfter);
                                Assertions.assertEquals(rBefore, rAfter);
                                Assertions.assertEquals(rAfter, val);
                                Assertions.assertEquals(bean.getValue(), val);
                            }).subscribe().with(x -> e.complete(val), e::fail);
                });
            }).map(i -> {
                Assertions.assertEquals(bean.getValue(), val);
                return new Foo(Integer.toString(i));
            });
        }

        @GET
        @Path("/blocking-server-and-reactive-client/{val}")
        public Foo blockingServerWithReactiveClient(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int rBefore = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(bean.getValue(), val);
                    client.invokeReactive(Integer.toString(val))
                            .invoke(s -> {
                                Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                                int rAfter = Vertx.currentContext().getLocal("count");
                                Assertions.assertEquals(s, "hello " + rAfter);
                                Assertions.assertEquals(rBefore, rAfter);
                                Assertions.assertEquals(rAfter, val);
                                Assertions.assertEquals(bean.getValue(), val);
                            }).subscribe().with(x -> e.complete(val), e::fail);
                });
            }).map(i -> {
                Assertions.assertEquals(bean.getValue(), val);
                return new Foo(Integer.toString(i));
            })
                    .await().indefinitely();
        }

        @GET
        @Path("/blocking-server-and-client/{val}")
        public Foo blockingServerAndBlockingClient(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int rBefore = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(bean.getValue(), val);
                    String s = client.invokeBlocking(Integer.toString(val));
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int rAfter = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(s, "hello " + rAfter);
                    Assertions.assertEquals(rBefore, rAfter);
                    Assertions.assertEquals(rAfter, val);
                    Assertions.assertEquals(bean.getValue(), val);
                    e.complete(val);
                }, true);
            }).map(i -> {
                Assertions.assertEquals(bean.getValue(), val);
                return new Foo(Integer.toString(i));
            })
                    .await().indefinitely();
        }

        @GET
        @Path("/reactive-server-and-blocking-client/{val}")
        public Uni<Foo> reactiveServerWithBlockingClient(int val) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Vertx.currentContext().putLocal("count", val);
            bean.setValue(val);

            return Uni.createFrom().<Integer> emitter(e -> {
                barrier.enqueue(Vertx.currentContext(), () -> {
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int rBefore = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(bean.getValue(), val);
                    String s = client.invokeBlocking(Integer.toString(val));
                    Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
                    int rAfter = Vertx.currentContext().getLocal("count");
                    Assertions.assertEquals(s, "hello " + rAfter);
                    Assertions.assertEquals(rBefore, rAfter);
                    Assertions.assertEquals(rAfter, val);
                    Assertions.assertEquals(bean.getValue(), val);
                    e.complete(val);
                }, true);
            }).map(i -> {
                Assertions.assertEquals(bean.getValue(), val);
                return new Foo(Integer.toString(i));
            });
        }
    }

    @Path("/remote")
    public static class RemoteService {

        @GET
        @Path("/reactive/{name}")
        public Uni<String> hello(String name) {
            return Uni.createFrom().item("hello " + name);
        }

        @GET
        @Path("/blocking/{name}")
        public String helloBlocking(String name) {
            return "hello " + name;
        }
    }

    @RegisterRestClient
    @Path("/remote")
    public interface RemoteClient {
        @GET
        @Path("/reactive/{name}")
        Uni<String> invokeReactive(String name);

        @GET
        @Path("/blocking/{name}")
        String invokeBlocking(String name);
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
            enqueue(context, runnable, false);
        }

        public void enqueue(Context context, Runnable runnable, boolean blocking) {
            Task task = new Task(context, runnable, blocking);
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

        private final boolean blocking;

        private Task(Context context, Runnable runnable, boolean blocking) {
            this.context = context;
            this.runnable = runnable;
            this.blocking = blocking;
        }

        void run() {
            if (blocking) {
                context.executeBlocking(p -> {
                    runnable.run();
                    p.complete();
                });
            } else {
                context.runOnContext(x -> runnable.run());
            }
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
