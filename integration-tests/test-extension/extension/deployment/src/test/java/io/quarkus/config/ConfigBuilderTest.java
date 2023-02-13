package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.StaticInitConfigBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class ConfigBuilderTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    SmallRyeConfig config;

    @Test
    void staticConfigBuilder() {
        assertEquals(1, StaticInitConfigBuilder.counter.get());
    }

    @Test
    void runTimeConfigBuilder() {
        assertEquals("1234", config.getRawValue("additional.builder.property"));
    }
}
