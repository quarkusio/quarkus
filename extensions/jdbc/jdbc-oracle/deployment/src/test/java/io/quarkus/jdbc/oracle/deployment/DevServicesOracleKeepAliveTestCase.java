package io.quarkus.jdbc.oracle.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesOracleKeepAliveTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.jdbc.enable-keep-alive", "true")
            .overrideConfigKey("quarkus.datasource.jdbc.read-timeout", "30S");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testKeepAliveEnabled() {
        AgroalConnectionFactoryConfiguration connectionFactoryConfig = dataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        Properties jdbcProperties = connectionFactoryConfig.jdbcProperties();
        assertThat(jdbcProperties.getProperty("oracle.net.keepAlive")).isEqualTo("true");
    }

    @Test
    public void testReadTimeoutSet() {
        AgroalConnectionFactoryConfiguration connectionFactoryConfig = dataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        Properties jdbcProperties = connectionFactoryConfig.jdbcProperties();
        assertThat(jdbcProperties.getProperty("oracle.jdbc.ReadTimeout")).isEqualTo("30000");
    }
}
