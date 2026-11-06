package io.quarkus.reactive.pg.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.sqlclient.Pool;

public class ChannelBindingRequireWithoutSslTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.channel-binding", "require")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.ssl-mode", "disable");

    @Inject
    Pool pool;

    @Test
    public void testConnectionFailsWithoutSsl() {
        assertThatThrownBy(() -> pool.query("SELECT 1").execute().await(10, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }
}
