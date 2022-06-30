package io.quarkus.it.resteasy.mutiny;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RegressionTest {

    @Nested
    @DisplayName("Regression tests for #25818 (see https://github.com/quarkusio/quarkus/issues/25818)")
    public class Bug25818 {

        @Test
        public void testDefaultExecutor() {
            get("/reproducer/25818/default-executor")
                    .then()
                    .body(is("hello-you"))
                    .statusCode(200);
        }

        @Test
        public void testWorkerPool() {
            get("/reproducer/25818/worker-pool")
                    .then()
                    .body(is("hello-you"))
                    .statusCode(200);
        }

        @Test
        public void yolo1() {
            get("/reproducer/25818/worker-pool-submit")
                    .then()
                    .body(is("yolo -> yolo"))
                    .statusCode(200);
        }

        @Test
        public void yolo2() {
            get("/reproducer/25818/worker-pool-schedule")
                    .then()
                    .body(is("yolo -> yolo"))
                    .statusCode(200);
        }
    }
}
