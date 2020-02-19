package io.quarkus.optaplanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class OptaPlannerProcessorIllegalXMLTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.optaplanner.solver-config-xml",
                    "io/quarkus/optaplanner/illegalScanAnnotatedSolverConfig.xml")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("io/quarkus/optaplanner/illegalScanAnnotatedSolverConfig.xml"))
            .assertException(throwable -> {
                assertTrue(throwable instanceof IllegalArgumentException);
                assertTrue(throwable.getMessage().contains("scanAnnotatedClasses"));
            });

    @Test
    public void scanAnnotatedClasses() {
        // Should not be called, deployment exception should happen first
        Assertions.fail();
    }
}
