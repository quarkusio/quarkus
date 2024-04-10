package io.quarkus.smallrye.faulttolerance.test.retry.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RetryOnAndRetryWhenExceptionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RetryOnAndRetryWhenExceptionService.class, IsIllegalArgumentException.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("Invalid @RetryWhen.exception"));
                assertTrue(e.getMessage().contains("must not be combined with @Retry.retryOn"));
            });

    @Test
    public void test() {
    }
}
