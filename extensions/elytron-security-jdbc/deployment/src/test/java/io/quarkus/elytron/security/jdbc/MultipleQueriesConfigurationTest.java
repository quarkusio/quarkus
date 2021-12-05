package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleQueriesConfigurationTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("multiple-queries/import.sql")
                    .addAsResource("multiple-queries/application.properties", "application.properties"));

}
