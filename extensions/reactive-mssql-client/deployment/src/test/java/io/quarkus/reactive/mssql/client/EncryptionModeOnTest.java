package io.quarkus.reactive.mssql.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class EncryptionModeOnTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-default-datasource.properties")
            .overrideConfigKey("quarkus.datasource.reactive.mssql.encryption-mode", "on")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem", "true")
            .overrideConfigKey("quarkus.datasource.reactive.trust-certificate-pem.certs",
                    "src/test/resources/tls/mssql.pem")
            .withApplicationRoot((jar) -> jar.addClasses(BeanUsingPool.class));

    @Inject
    BeanUsingPool bean;

    @Test
    public void testPoolWorks() throws Exception {
        RowSet<Row> rows = bean.executeQuery();
        assertEquals(1, rows.size());
        Row row = rows.iterator().next();
        String encrypted = row.getString("encrypt_option");
        assertEquals("true", encrypted.toLowerCase(Locale.ENGLISH));
    }

    @ApplicationScoped
    static class BeanUsingPool {

        @Inject
        Pool pool;

        public RowSet<Row> executeQuery() throws Exception {
            return pool.query("SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id = @@SPID").execute()
                    .await(10, TimeUnit.SECONDS);

        }
    }
}
