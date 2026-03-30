package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MinimalConfigurationTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("minimal-config/import.sql")
                    .addAsResource("minimal-config/application.properties", "application.properties"));

}
