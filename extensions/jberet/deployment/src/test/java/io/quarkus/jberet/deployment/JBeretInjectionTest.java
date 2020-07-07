package io.quarkus.jberet.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.jberet.repository.JobRepository;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JBeretInjectionTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    JobOperator jobOperator;
    @Inject
    JobRepository jobRepository;

    @Test
    void injection() {
        assertNotNull(jobOperator);
        assertNotNull(jobRepository);
    }
}
