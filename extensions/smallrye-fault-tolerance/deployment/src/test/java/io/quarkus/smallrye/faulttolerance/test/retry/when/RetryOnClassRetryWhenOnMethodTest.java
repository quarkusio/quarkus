package io.quarkus.smallrye.faulttolerance.test.retry.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RetryOnClassRetryWhenOnMethodTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RetryOnClassRetryWhenOnMethodService.class))
            .assertException(e -> {
                assertEquals(DefinitionException.class, e.getClass());
                assertTrue(e.getMessage().contains("@RetryWhen present"));
                assertTrue(e.getMessage().contains("@Retry is missing"));
            });

    @Test
    public void test() {
    }
}
