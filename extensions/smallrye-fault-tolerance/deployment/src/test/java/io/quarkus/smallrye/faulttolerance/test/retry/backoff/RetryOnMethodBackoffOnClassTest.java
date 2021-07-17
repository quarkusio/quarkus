package io.quarkus.smallrye.faulttolerance.test.retry.backoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DefinitionException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RetryOnMethodBackoffOnClassTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RetryOnMethodBackoffOnClassService.class))
            .assertException(e -> {
                assertEquals(DefinitionException.class, e.getClass());
                assertTrue(e.getMessage().contains("Backoff annotation"));
                assertTrue(e.getMessage().contains("@Retry is missing"));
            });

    @Test
    public void test() {
        fail();
    }
}
