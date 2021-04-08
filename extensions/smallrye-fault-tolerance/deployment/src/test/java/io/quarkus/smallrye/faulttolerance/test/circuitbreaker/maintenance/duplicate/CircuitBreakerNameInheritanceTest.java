package io.quarkus.smallrye.faulttolerance.test.circuitbreaker.maintenance.duplicate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;

public class CircuitBreakerNameInheritanceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SubCircuitBreakerService.class, SuperCircuitBreakerService.class));

    @Inject
    CircuitBreakerMaintenance cb;

    @Test
    public void deploysWithoutError() {
        assertNotNull(cb);
    }
}
