package io.quarkus.it.opentelemetry.minimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloServiceTest {

    @Inject
    HelloService helloService;

    @Test
    public void testHello() {
        Integer result = helloService.getMetricCount().await().atMost(Duration.ofSeconds(5));
        System.out.println("Metric count: " + result);
        assertThat(result, greaterThanOrEqualTo(1));
    }
}
