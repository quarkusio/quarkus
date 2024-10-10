package io.quarkus.reactive.mssql.client;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;

public class SamePoolInstanceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    io.vertx.mutiny.mssqlclient.MSSQLPool mutinyPool;
    @Inject
    io.vertx.mssqlclient.MSSQLPool pool;

    @Test
    public void test() {
        Assertions.assertThat(ClientProxy.unwrap(pool)).isSameAs(ClientProxy.unwrap(mutinyPool.getDelegate()));
    }
}
