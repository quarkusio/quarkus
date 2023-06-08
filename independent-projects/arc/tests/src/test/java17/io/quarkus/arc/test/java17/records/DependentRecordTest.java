package io.quarkus.arc.test.java17.records;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependentRecordTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(DependentRecord.class);

    @Test
    public void test() {
        assertNotNull(Arc.container().select(DependentRecord.class).get());
    }

    @Dependent
    record DependentRecord() {
    }
}
