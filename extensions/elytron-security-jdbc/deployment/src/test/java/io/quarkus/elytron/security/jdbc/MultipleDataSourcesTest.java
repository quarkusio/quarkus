package io.quarkus.elytron.security.jdbc;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleDataSourcesTest extends JdbcSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("multiple-data-sources/users.sql")
                    .addAsResource("multiple-data-sources/permissions.sql")
                    .addAsResource("multiple-data-sources/application.properties", "application.properties"));

}
