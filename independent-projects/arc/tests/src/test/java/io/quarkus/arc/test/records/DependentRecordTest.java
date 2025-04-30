package io.quarkus.arc.test.records;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

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
