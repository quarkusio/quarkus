package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionConfigDefaultDataSourceTest {

    @Inject
    Flyway flyway;

    @Inject
    FlywayExtensionConfigFixture fixture;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FlywayExtensionConfigFixture.class)
                    .addAsResource("config-for-default-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Reads flyway configuration for default datasource correctly")
    public void testFlywayConfigInjection() {
        fixture.assertAllConfigurationSettings(flyway.getConfiguration(), "");
        assertFalse(fixture.migrateAtStart(""));
    }
}
