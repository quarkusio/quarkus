package io.quarkus.reactive.pg.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class ChannelBindingRequireWithSslTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.channel-binding", "require")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.ssl-mode", "require")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem", "true")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem.certs",
                    "target/certificates/postgres-server-ca.crt")
            .withApplicationRoot(jar -> jar.addClass(BeanUsingPool.class));

    @Inject
    BeanUsingPool bean;

    @Test
    public void testConnectionIsEstablishedWithSsl() throws Exception {
        RowSet<Row> rows = bean.executeQuery();
        Row row = rows.iterator().next();
        assertThat(row.getBoolean("ssl")).as("Expected SSL to be active for this connection").isTrue();
    }

    @ApplicationScoped
    static class BeanUsingPool {

        @Inject
        Pool pool;

        public RowSet<Row> executeQuery() throws Exception {
            return pool.query("SELECT ssl FROM pg_stat_ssl WHERE pid = pg_backend_pid()")
                    .execute().await(10, TimeUnit.SECONDS);
        }
    }
}
