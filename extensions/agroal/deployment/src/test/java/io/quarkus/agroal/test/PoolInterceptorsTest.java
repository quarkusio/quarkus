package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class PoolInterceptorsTest {

    //tag::injection[]
    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("another")
    AgroalDataSource anotherDataSource;

    @Inject
    @DataSource("pure")
    AgroalDataSource pureDataSource;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addClasses(PoolInterceptorsTest.class)
                    .addClasses(DefaultInterceptor.class, AnotherInterceptor.class, AnotherPriorityInterceptor.class))
            .withConfigurationResource("application-pool-interception.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        try (Connection connection = defaultDataSource.getConnection()) {
            Assertions.assertEquals("INTERCEPTOR", connection.getSchema());
        }
        try (Connection connection = anotherDataSource.getConnection()) {
            Assertions.assertEquals("PRIORITY", connection.getSchema());
        }
        try (Connection connection = pureDataSource.getConnection()) {
            Assertions.assertEquals("PUBLIC", connection.getSchema());
        }
    }

    // --- //

    @Singleton
    static class DefaultInterceptor implements AgroalPoolInterceptor {

        @Override
        public void onConnectionAcquire(Connection connection) {
            try {
                connection.setSchema("INTERCEPTOR");
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        }
    }

    @Singleton
    @DataSource("another")
    static class AnotherInterceptor implements AgroalPoolInterceptor {

        @Override
        public void onConnectionAcquire(Connection connection) {
            try {
                connection.setSchema("LOW");
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        }
    }

    @Singleton
    @DataSource("another")
    static class AnotherPriorityInterceptor implements AgroalPoolInterceptor {

        @Override
        public void onConnectionAcquire(Connection connection) {
            try {
                Assertions.assertEquals("LOW", connection.getSchema());
                connection.setSchema("PRIORITY");
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        }

        @Override
        public int getPriority() {
            return 1;
        }
    }
}
