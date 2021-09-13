package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;

public class StaticInitConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(StaticInitConfigSource.class)
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            StaticInitConfigSource.class.getName()));

    // This does not come from the static init Config, but it is registered in both static and runtime.
    // If it doesn't fail, it means that the static mapping was done correctly.
    @Inject
    StaticInitConfigMapping mapping;

    @Test
    void staticInitMapping() {
        assertEquals("1234", mapping.myProp());
    }

    @ConfigMapping(prefix = "config.static.init")
    public interface StaticInitConfigMapping {
        String myProp();
    }
}
