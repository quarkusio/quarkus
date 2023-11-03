package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Liquibase needs a datasource to work.
 * This tests assures, that an error occurs, as soon as a named liquibase configuration points to a missing datasource.
 */
public class LiquibaseExtensionConfigMissingNamedDataSourceTest {

    @Inject
    @LiquibaseDataSource("users")
    Instance<LiquibaseFactory> liquibase;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("config-for-missing-named-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Injecting liquibase should fail if the named datasource is missing")
    public void testLiquibaseNotAvailableWithoutDataSource() {
        assertThrows(UnsatisfiedResolutionException.class, liquibase::get);
    }
}
