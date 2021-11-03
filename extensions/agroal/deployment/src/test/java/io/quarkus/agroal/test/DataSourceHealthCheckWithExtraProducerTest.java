package io.quarkus.agroal.test;

import static java.lang.annotation.ElementType.FIELD;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.inject.Qualifier;
import javax.sql.DataSource;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Check that if an application code contains an extra CDI bean that implements javax.sql.DataSource,
 * but it is not a data source configured through regular configuration means, then it is not included in
 * health checks.
 */
public class DataSourceHealthCheckWithExtraProducerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExtraDataSourceProducer.class, ExtraDataSource.class))
            .withConfigurationResource("application-datasources-with-health.properties");

    static class ExtraDataSourceProducer {

        @Produces
        @ExtraDataSource
        DataSource extraDs = new DataSource() {

            @Override
            public Connection getConnection() throws SQLException {
                throw new IllegalStateException();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw new IllegalStateException();
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }
        };

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ FIELD })
    static @interface ExtraDataSource {

    }

    @Test
    public void testDataSourceHealthCheckExclusion() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .body("status", CoreMatchers.equalTo("UP"));
    }

}
