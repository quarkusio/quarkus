package io.quarkus.smallrye.faulttolerance.test.retry.backoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TwoBackoffsOnMethodTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TwoBackoffsOnMethodService.class))
            .assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertTrue(e.getMessage().contains("More than one backoff defined"));
            });

    @Test
    public void test() {
        fail();
    }
}
