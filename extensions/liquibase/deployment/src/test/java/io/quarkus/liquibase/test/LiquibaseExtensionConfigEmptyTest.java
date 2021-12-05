package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Liquibase needs a datasource to work.
 * This tests assures, that an error occurs,
 * as soon as the default liquibase configuration points to an missing default datasource.
 */
public class LiquibaseExtensionConfigEmptyTest {

    @Inject
    Instance<LiquibaseFactory> liquibase;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-empty.properties", "application.properties"));

    @Test
    @DisplayName("Injecting (default) liquibase should fail if there is no datasource configured")
    public void testLiquibaseNotAvailableWithoutDataSource() {
        assertThrows(UnsatisfiedResolutionException.class, () -> liquibase.get().getConfiguration());
    }
}
