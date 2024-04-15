package io.quarkus.load.shedding;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NaiveLoadSheddingTest {
    private static final int NUM_THREADS = 20;
    private static final int NUM_REQUESTS = 10;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class))
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "5")
            .overrideConfigKey("quarkus.load-shedding.max-limit", "10")
            .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false");

    @Test
    public void test() throws InterruptedException {
        AtomicInteger numErrors = new AtomicInteger();
        CountDownLatch begin = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(() -> {
                try {
                    begin.await();
                    for (int j = 0; j < NUM_REQUESTS; j++) {
                        int statusCode = when().get("/").then().extract().statusCode();
                        if (statusCode == 503) {
                            numErrors.incrementAndGet();
                        }
                    }
                    end.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        begin.countDown();
        end.await();

        // at least 1/2 of all requests failed
        assertThat(numErrors).hasValueGreaterThanOrEqualTo(100);
    }

    @Path("/")
    public static class MyResource {
        @GET
        public String hello() throws InterruptedException {
            Thread.sleep(100);
            return "Hello, world!";
        }
    }
}
