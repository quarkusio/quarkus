package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;

public class UnremovedConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(UnremovedConfigMapping.class));

    @Inject
    SmallRyeConfig config;
    @Inject
    Base base;

    @Test
    void unremoved() {
        UnremovedConfigMapping mapping = config.getConfigMapping(UnremovedConfigMapping.class);
        assertEquals("1234", mapping.prop());

        mapping = CDI.current().select(UnremovedConfigMapping.class).get();
        assertEquals("1234", mapping.prop());

        UnremovedConfigProperties properties = CDI.current().select(UnremovedConfigProperties.class).get();
        assertEquals("1234", properties.prop);
    }

    @Test
    void unremovedInjectionPointByParentType() {
        assertNotNull(base);
        assertEquals("default", base.base());
    }

    @Unremovable
    @ConfigMapping(prefix = "mapping")
    public interface UnremovedConfigMapping {
        @WithDefault("1234")
        String prop();
    }

    public interface Base {
        @WithDefault("default")
        String base();
    }

    @ConfigMapping(prefix = "base")
    public interface ExtendsBase extends Base {
        @WithDefault("default")
        String myProp();
    }

    @Unremovable
    @ConfigProperties(prefix = "mapping")
    public static class UnremovedConfigProperties {
        String prop;
    }
}
