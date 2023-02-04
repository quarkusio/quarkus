package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.Funq;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class RequestScopeLeakTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, Identity.class, Greeting.class, MyFunction.class)
                    .addAsResource("greeting-uni.properties", "application.properties"));

    @BeforeEach
    void cleanup() {
        MyBean.DISPOSED.set(0);
    }

    @Test
    public void testRequestScope() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"Roxanne\"}")
                .post("/")
                .then().statusCode(200)
                .header("ce-id", nullValue())
                .body("name", equalTo("Roxanne"))
                .body("message", equalTo("Hello Roxanne!"));

        Assertions.assertEquals(1, MyBean.DISPOSED.get());
    }

    @Test
    public void testRequestScopeWithSyncFailure() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"sync-failure\"}")
                .post("/")
                .then().statusCode(500);
        Assertions.assertEquals(1, MyBean.DISPOSED.get());
    }

    @Test
    public void testRequestScopeWithSyncFailureInPipeline() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"sync-failure-pipeline\"}")
                .post("/")
                .then().statusCode(500);
        Assertions.assertEquals(1, MyBean.DISPOSED.get());
    }

    @Test
    public void testRequestScopeWithASyncFailure() {
        RestAssured.given().contentType("application/json")
                .body("{\"name\": \"async-failure\"}")
                .post("/")
                .then().statusCode(500);
        Assertions.assertEquals(1, MyBean.DISPOSED.get());
    }

    @RequestScoped
    public static class MyBean {
        public static AtomicInteger DISPOSED = new AtomicInteger();

        private final AtomicInteger counter = new AtomicInteger();

        public int inc() {
            return counter.getAndIncrement();
        }

        public void get() {
            counter.get();
        }

        @PreDestroy
        public void destroy() {
            DISPOSED.incrementAndGet();
        }
    }

    public static class MyFunction {

        @Inject
        MyBean bean;
        @Inject
        Vertx vertx;

        @Funq
        public Uni<Greeting> greeting(Identity name) {
            Context context = Vertx.currentContext();
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());

            if (name.getName().equals("sync-failure")) {
                Assertions.assertEquals(0, bean.inc());
                throw new IllegalArgumentException("expected sync-failure");
            }

            return Uni.createFrom().item("Hello " + name.getName() + "!")
                    .invoke(() -> {
                        Assertions.assertEquals(0, bean.inc());
                        Assertions.assertSame(context, Vertx.currentContext());
                    })
                    .chain(this::nap)
                    .invoke(() -> {
                        Assertions.assertEquals(1, bean.inc());
                        Assertions.assertSame(context, Vertx.currentContext());
                    })
                    .invoke(() -> {
                        if (name.getName().equals("sync-failure-pipeline")) {
                            throw new IllegalArgumentException("expected sync-failure-in-pipeline");
                        }
                    })
                    .map(s -> {
                        Greeting greeting = new Greeting();
                        greeting.setName(name.getName());
                        greeting.setMessage(s);
                        return greeting;
                    })
                    .chain(greeting -> {
                        if (greeting.getName().equals("async-failure")) {
                            return Uni.createFrom().failure(() -> new IllegalArgumentException("expected async-failure"));
                        }
                        return Uni.createFrom().item(greeting);
                    });
        }

        public Uni<String> nap(String s) {
            return Uni.createFrom().emitter(e -> {
                vertx.setTimer(100, x -> e.complete(s));
            });
        }

    }
}
