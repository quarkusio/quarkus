package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoBeforeRetryMethodFoundTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NoBeforeRetryMethodFoundService.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("Invalid @BeforeRetry"));
                assertTrue(e.getMessage().contains("can't find before retry method"));
            });

    @Test
    public void test() {
    }
}
