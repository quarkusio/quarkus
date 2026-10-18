package io.quarkus.jdbc.mssql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesMsSQLReadTimeoutTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.read-timeout", "30S");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testReadTimeoutSet() {
        AgroalConnectionFactoryConfiguration connectionFactoryConfig = dataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        Properties jdbcProperties = connectionFactoryConfig.jdbcProperties();
        assertThat(jdbcProperties.getProperty("socketTimeout")).isEqualTo("30000");
    }
}
