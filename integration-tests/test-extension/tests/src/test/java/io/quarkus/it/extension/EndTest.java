package io.quarkus.it.extension;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@CustomResource
@QuarkusTest
public class EndTest {

    @Test
    public void test1() {
        assertTrue(Counter.endCounter.get() <= 1);
    }

    @Test
    public void test2() {
        assertTrue(Counter.endCounter.get() <= 1);
    }

}
