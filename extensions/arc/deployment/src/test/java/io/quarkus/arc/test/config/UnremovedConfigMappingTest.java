package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;

public class UnremovedConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(UnremovedConfigMapping.class));

    @Inject
    SmallRyeConfig config;
    @Inject
    Base base;

    @Test
    void unremoved() {
        UnremovedConfigMapping mapping = config.getConfigMapping(UnremovedConfigMapping.class);
        assertEquals("1234", mapping.prop());
    }

    @Test
    void unremovedInjectionPointByParentType() {
        assertNotNull(base);
        assertEquals("default", base.base());
    }

    @Test
    void unremovedWithInjectionPoint() {
        assertTrue(Arc.container().instance(Other.class).isAvailable());
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
    @ConfigMapping(prefix = "other")
    public interface Other {

    }
}
