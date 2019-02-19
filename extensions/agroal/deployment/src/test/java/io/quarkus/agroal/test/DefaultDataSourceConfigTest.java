package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusUnitTest;

@Disabled
public class DefaultDataSourceConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsManifestResource("microprofile-config-default-datasource.properties",
                            "microprofile-config.properties"));

    @Test
    public void testDefaultDataSourceInjection() throws SQLException {
        testDataSource(defaultDataSource, "username-default", 3, 13);
    }

    private static void testDataSource(AgroalDataSource dataSource, String username, int minSize, int maxSize)
            throws SQLException {
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();

        assertEquals("jdbc:h2:tcp://localhost/mem:default", configuration.connectionFactoryConfiguration().jdbcUrl());
        assertEquals(username, configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(minSize, configuration.minSize());
        assertEquals(maxSize, configuration.maxSize());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
