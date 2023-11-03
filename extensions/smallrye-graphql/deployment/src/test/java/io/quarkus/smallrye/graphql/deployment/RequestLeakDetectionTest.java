package io.quarkus.smallrye.graphql.deployment;

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

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
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

public class RequestLeakDetectionTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(RequestLeakDetectionTest.MyGraphQLApi.class, MyRequestScopeBean.class, Barrier.class, Task.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    Barrier barrier;

    @Test
    public void testWithConcurrentCalls() {
        List<String> results = new CopyOnWriteArrayList<>();
        List<String> nested = new CopyOnWriteArrayList<>();
        List<String> nestedUni = new CopyOnWriteArrayList<>();
        int count = 100;
        barrier.setMaxConcurrency(count);
        for (int i = 0; i < count; i++) {
            int c = i;
            new Thread(() -> {
                String query = getPayload("{ foo(val:" + c + ") { value nested{ value } nestedUni { value } } }");
                ResponseBody<?> body = RestAssured.given().body(query).contentType(MEDIATYPE_JSON).post("/graphql/")
                        .thenReturn().body();
                String value = body.path("data.foo.value");
                String nestedValue = body.path("data.foo.nested.value");
                String nestedUniValue = body.path("data.foo.nestedUni.value");
                results.add(value);
                nested.add(nestedValue);
                nestedUni.add(nestedUniValue);
            }).start();
        }
        await().until(() -> results.size() == count);
        await().until(() -> nested.size() == count);
        await().until(() -> nestedUni.size() == count);
        Set<String> asSet = new HashSet<>(results);
        Assertions.assertEquals(count, asSet.size());
        asSet = new HashSet<>(nested);
        Assertions.assertEquals(count, asSet.size());
        asSet = new HashSet<>(nestedUni);
        Assertions.assertEquals(count, asSet.size());
    }

    @ApplicationScoped
    @GraphQLApi
    public static class MyGraphQLApi {

        @Inject
        MyRequestScopeBean bean;

        @Inject
        Barrier barrier;

        @Query
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

        public Foo nested(@Source Foo foo) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            int r = Vertx.currentContext().getLocal("count");
            String rAsString = Integer.toString(r);
            Assertions.assertEquals(rAsString, foo.value);
            Assertions.assertEquals(bean.getValue(), r);
            return new Foo("source field on foo " + foo.value);
        }

        public Uni<Foo> nestedUni(@Source Foo foo) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            int r = Vertx.currentContext().getLocal("count");
            String rAsString = Integer.toString(r);
            Assertions.assertEquals(rAsString, foo.value);
            Assertions.assertEquals(bean.getValue(), r);
            return Uni.createFrom().item(new Foo("uni source field on foo " + foo.value));
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
