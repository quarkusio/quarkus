package org.jboss.shamrock.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.shamrock.agroal.runtime.DataSource;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;

public class MultipleDataSourcesConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("datasource1")
    AgroalDataSource dataSource1;

    @Inject
    @DataSource("datasource2")
    AgroalDataSource dataSource2;

    @RegisterExtension
    static final ShamrockUnitTest config = new ShamrockUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("microprofile-config-multiple-datasources.properties", "microprofile-config.properties"));

    @Test
    public void testDataSourceInjection() throws SQLException {
        testDataSource("default", defaultDataSource, "jdbc:h2:tcp://localhost/mem:default", "username-default", 3, 13);
        testDataSource("datasource1", dataSource1, "jdbc:h2:tcp://localhost/mem:datasource1", "username1", 1, 11);
        testDataSource("datasource2", dataSource2, "jdbc:h2:tcp://localhost/mem:datasource2", "username2", 2, 12);
    }

    private static void testDataSource(String dataSourceName, AgroalDataSource dataSource, String jdbcUrl, String username, int minSize, int maxSize)
            throws SQLException {
        AgroalConnectionPoolConfiguration configuration = null;

        try {
            configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        } catch (NullPointerException e) {
            // we catch the NPE here as we have a proxy and we can't test dataSource directly
            fail("Datasource " + dataSourceName + " should not be null");
        }
        assertEquals(jdbcUrl, configuration.connectionFactoryConfiguration().jdbcUrl());
        assertEquals(username, configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(minSize, configuration.minSize());
        assertEquals(maxSize, configuration.maxSize());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
