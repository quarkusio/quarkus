package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class QuarkusUnitTestWithRuntimeConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withRuntimeConfiguration("""
                        test.inline.runtime=value
                        quarkus.log.category.test.category.level=DEBUG
                    """);

    @Test
    public void testInlineConfigurationApplied() {
        Config cfg = ConfigProvider.getConfig();

        assertEquals("value", cfg.getValue("test.inline.runtime", String.class));
        assertEquals("DEBUG", cfg.getValue("quarkus.log.category.test.category.level", String.class));
    }
}
