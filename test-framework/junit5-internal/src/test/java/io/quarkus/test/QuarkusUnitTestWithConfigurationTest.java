package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class QuarkusUnitTestWithConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfiguration("""
                        quarkus.datasource.db-kind=postgresql
                        quarkus.datasource.username=quarkus
                        quarkus.datasource.password=quarkus
                    """);

    @Test
    public void testInlineConfigurationApplied() {
        Config cfg = ConfigProvider.getConfig();

        assertEquals("postgresql", cfg.getValue("quarkus.datasource.db-kind", String.class));
        assertEquals("quarkus", cfg.getValue("quarkus.datasource.username", String.class));
        assertEquals("quarkus", cfg.getValue("quarkus.datasource.password", String.class));
    }
}
