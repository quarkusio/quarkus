package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.TestMappingRunTime;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class RecorderRuntimeConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    SmallRyeConfig config;
    @Inject
    TestMappingRunTime mappingRunTime;

    @Test
    void runtimeConfig() {
        // Make sure we get the recorded property with the highest priority (the profile property with test)
        assertEquals("from-application", config.getConfigValue("recorded.property").getValue());
        assertEquals("from-application", config.getConfigValue("recorded.profiled.property").getValue());
        assertEquals("from-application", config.getConfigValue("quarkus.mapping.rt.record-profiled").getValue());
        assertTrue(mappingRunTime.recordProfiled().isPresent());
        assertEquals("from-application", mappingRunTime.recordProfiled().get());

        // Make sure that when we merge all the defaults, we override the non-profiled name
        Optional<ConfigSource> configSource = config.getConfigSource("Runtime Values");
        assertTrue(configSource.isPresent());
        ConfigSource runtimeValues = configSource.get();
        assertEquals("from-application", runtimeValues.getValue("%test.recorded.profiled.property"));
        assertEquals("recorded", runtimeValues.getValue("recorded.profiled.property"));
        assertEquals("from-application", runtimeValues.getValue("%test.quarkus.mapping.rt.record-profiled"));
        assertEquals("recorded", runtimeValues.getValue("quarkus.mapping.rt.record-profiled"));
    }
}
