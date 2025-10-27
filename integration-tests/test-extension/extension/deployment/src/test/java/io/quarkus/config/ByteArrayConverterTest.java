package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ByteArrayConverterTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("someapp.bytearray", "XyZ*123")
            .failOnUnknownProperties(false); // quarkus.build.unknown.prop from UnknownBuildPropertyConfigSourceFactory

    @ConfigProperty(name = "someapp.bytearray")
    protected byte[] bytearray;

    @Test
    void buildTimeConfigBuilder() {
        assertEquals("XyZ*123", new String(bytearray, StandardCharsets.UTF_8));
    }
}
