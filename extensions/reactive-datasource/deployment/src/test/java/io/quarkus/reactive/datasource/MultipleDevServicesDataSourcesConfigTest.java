package io.quarkus.reactive.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.builder.Version;
import io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.sqlclient.Pool;

@DisabledOnOs(OS.WINDOWS)
public class MultipleDevServicesDataSourcesConfigTest {

    //tag::injection[]
    @Inject
    Pool defaultDataSource;

    @Inject
    @ReactiveDataSource("users")
    Pool dataSource1;

    @Inject
    @ReactiveDataSource("inventory")
    Pool dataSource2;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-pg-client-deployment", Version.getVersion())))
            .withConfigurationResource("application-multiple-devservices-datasources.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        Pool unwrap = ClientProxy.unwrap(dataSource1);

        testDataSource(DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME, defaultDataSource);
        testDataSource("users", dataSource1);
        testDataSource("inventory", dataSource2);
    }

    static void testDataSource(String dataSourceName, Pool dataSource)
            throws SQLException {
        assertThat(dataSource).isNotNull();
    }
}
