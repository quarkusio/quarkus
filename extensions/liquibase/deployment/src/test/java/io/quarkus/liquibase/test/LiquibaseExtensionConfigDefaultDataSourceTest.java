package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionConfigDefaultDataSourceTest {

    @Inject
    LiquibaseFactory liquibase;

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/xml/changeLog.xml")
                    .addAsResource("db/xml/create-tables.xml")
                    .addAsResource("db/xml/create-views.xml")
                    .addAsResource("db/xml/test/test.xml")
                    .addAsResource("config-for-default-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Reads liquibase configuration for default datasource correctly")
    public void testLiquibaseConfigInjection() {
        fixture.assertAllConfigurationSettings(liquibase.getConfiguration(), "");
        assertFalse(fixture.migrateAtStart(""));
    }
}
