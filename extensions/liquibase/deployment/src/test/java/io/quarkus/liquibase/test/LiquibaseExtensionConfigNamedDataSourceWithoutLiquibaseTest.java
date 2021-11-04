package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Assures, that liquibase can also be used without any configuration,
 * provided, that at least a named datasource is configured.
 */
public class LiquibaseExtensionConfigNamedDataSourceWithoutLiquibaseTest {

    @Inject
    @LiquibaseDataSource("users")
    LiquibaseFactory liquibaseFactory;

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @Inject
    @DataSource("users")
    AgroalDataSource usersDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("config-for-named-datasource-without-liquibase.properties", "application.properties"));

    @Test
    @DisplayName("Reads predefined default liquibase configuration for named datasource correctly")
    public void testLiquibaseDefaultConfigInjection() throws Exception {
        fixture.assertDefaultConfigurationSettings(liquibaseFactory.getConfiguration());
        assertFalse(fixture.migrateAtStart("users"));
    }
}
