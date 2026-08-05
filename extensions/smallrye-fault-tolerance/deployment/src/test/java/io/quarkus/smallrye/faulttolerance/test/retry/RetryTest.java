package io.quarkus.smallrye.faulttolerance.test.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class RetryTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(RetryBean.class));

    @Inject
    RetryBean retry;

    @Test
    public void test() {
        assertEquals(4, retry.hello());
    }
}
