package io.quarkus.it.main;

import static org.hamcrest.Matchers.contains;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JaxbTestCase {

    private static final AtomicInteger count = new AtomicInteger(0);

    @BeforeEach
    public void beforeInEnclosing() {
        count.incrementAndGet();
    }

    @Nested
    class SomeClass {

        @BeforeEach
        public void beforeInTest() {
            count.incrementAndGet();
        }

        @Test
        public void testNews() {
            RestAssured.when().get("/test/jaxb/getnews").then()
                    .body("author", contains("Emmanuel Bernard"));
            Assertions.assertEquals(2, count.get());
        }
    }

}
