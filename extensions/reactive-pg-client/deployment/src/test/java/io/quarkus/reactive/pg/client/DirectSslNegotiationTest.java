package io.quarkus.reactive.pg.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class DirectSslNegotiationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.ssl-negotiation", "direct")
            .overrideConfigKey("quarkus.datasource.reactive.postgresql.ssl-mode", "require")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem", "true")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem.certs",
                    "src/test/resources/tls/server.crt")
            .withApplicationRoot(jar -> jar.addClass(BeanUsingPool.class));

    @Inject
    BeanUsingPool bean;

    @Test
    public void testSslConnectionIsEstablished() throws Exception {
        RowSet<Row> rows = bean.executeQuery();
        Row row = rows.iterator().next();
        assertTrue(row.getBoolean("ssl"), "Expected SSL to be active for this connection");
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
