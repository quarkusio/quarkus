package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BothValueAndMethodNameSetTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BothValueAndMethodNameSetService.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("Invalid @BeforeRetry"));
                assertTrue(e.getMessage().contains(
                        "before retry handler class and before retry method can't be specified both at the same time"));
            });

    @Test
    public void test() {
    }
}
