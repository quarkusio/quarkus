package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.TestMappingBuildTimeRunTime;
import io.quarkus.extest.runtime.config.TestMappingRunTime;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties"));

    @Inject
    TestMappingBuildTimeRunTime mappingBuildTimeRunTime;
    @Inject
    TestMappingRunTime mappingRunTime;

    @Test
    void mappingBuildTimeRunTime() {
        assertEquals("value", mappingBuildTimeRunTime.value());
    }

    @Test
    void mappingRunTime() {
        assertEquals("value", mappingRunTime.value());
    }
}
