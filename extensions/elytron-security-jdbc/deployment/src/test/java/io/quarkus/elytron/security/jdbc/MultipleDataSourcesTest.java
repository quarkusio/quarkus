package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleDataSourcesTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("multiple-data-sources/users.sql")
                    .addAsResource("multiple-data-sources/permissions.sql")
                    .addAsResource("multiple-data-sources/application.properties", "application.properties"));

}
