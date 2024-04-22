package io.quarkus.arc.test.records;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class NormalScopedRecordTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(NormalScopedRecord.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("records are always final"));
    }

    @ApplicationScoped
    record NormalScopedRecord() {
    }
}
