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

public class RequestScopeTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, Identity.class, Greeting.class, MyFunction.class)
                    .addAsResource("greeting.properties", "application.properties"));

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
    public void testRequestScopeTerminationWithSynchronousFailure() {
        String body = RestAssured.given().contentType("application/json")
                .body("{\"name\": \"failure\"}")
                .post("/")
                .then().statusCode(500).extract().asString();

        Assertions.assertTrue(body.contains("expected failure"));
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

        @Funq
        public Greeting greet(Identity name) {
            Assertions.assertTrue(VertxContext.isOnDuplicatedContext());
            Assertions.assertEquals(0, bean.inc());

            if (name.getName().equals("failure")) {
                throw new IllegalArgumentException("expected failure");
            }

            Greeting greeting = new Greeting();
            greeting.setName(name.getName());
            greeting.setMessage("Hello " + name.getName() + "!");

            Assertions.assertEquals(1, bean.inc());
            return greeting;
        }

    }
}
