package io.quarkus.smallrye.faulttolerance.test.retry.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class RetryStackOverflowTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(RetryStackOverflowService.class));

    @Inject
    RetryStackOverflowService service;

    @Test
    public void test() {
        assertEquals("fallback", service.hello());
    }
}
