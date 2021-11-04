package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Assures, that Liquibase can also be used without any configuration,
 * provided, that at least a datasource is configured.
 */
public class LiquibaseExtensionConfigDefaultDataSourceWithoutLiquibaseTest {

    @Inject
    LiquibaseFactory liquibase;

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("config-for-default-datasource-without-liquibase.properties", "application.properties"));

    @Test
    @DisplayName("Reads predefined default liquibase configuration for default datasource correctly")
    public void testLiquibaseDefaultConfigInjection() {
        fixture.assertDefaultConfigurationSettings(liquibase.getConfiguration());
        assertFalse(fixture.migrateAtStart(""));
    }
}
