package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusExtensionTest;

public class H2DatabaseTestResourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:h2_test_resource_compat");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testDatasourceUsesH2TestResource() throws Exception {
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration()
                .connectionPoolConfiguration();
        String url = configuration.connectionFactoryConfiguration().jdbcUrl();
        assertThat(url).contains("mem:h2_test_resource_compat");
        assertThat(url).doesNotContain("mem:quarkus");
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SELECT 1");
        }
    }
}
