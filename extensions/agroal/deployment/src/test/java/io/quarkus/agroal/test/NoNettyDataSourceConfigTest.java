package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class NoNettyDataSourceConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties")
            //this is a bit yuck, but lots of deps used by other tests bring in Netty as transitive dependencies
            //we need to exclude them all
            //this removes the need to have a whole new module just for this test
            .overrideConfigKey("quarkus.class-loading.removed-artifacts",
                    "io.netty:netty-common,io.quarkus:quarkus-vertx,io.quarkus:quarkus-vertx-deployment," +
                            "io.quarkus:quarkus-resteasy-deployment,io.quarkus:quarkus-resteasy," +
                            "io.quarkus:quarkus-resteasy-server-common-deployment, io.quarkus:quarkus-resteasy-server-common," +
                            "io.quarkus:quarkus-vertx-http,io.quarkus:quarkus-vertx-http-deployment," +
                            "io.quarkus:quarkus-netty,io.quarkus:quarkus-netty-deployment," +
                            "io.quarkus:quarkus-smallrye-metrics,io.quarkus:quarkus-smallrye-metrics-deployment," +
                            "io.quarkus:quarkus-smallrye-open-api,io.quarkus:quarkus-smallrye-open-api-deployment," +
                            "io.quarkus:quarkus-smallrye-health-deployment,io.quarkus:quarkus-smallrye-health,");

    @Test
    public void testNoNetty() throws SQLException {
        AgroalConnectionPoolConfiguration configuration = defaultDataSource.getConfiguration().connectionPoolConfiguration();

        try (Connection connection = defaultDataSource.getConnection()) {
        }
    }
}
