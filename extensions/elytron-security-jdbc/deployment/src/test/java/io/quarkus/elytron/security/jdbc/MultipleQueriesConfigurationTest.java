package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleQueriesConfigurationTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("multiple-queries/import.sql")
                    .addAsResource("multiple-queries/application.properties", "application.properties"));

}
