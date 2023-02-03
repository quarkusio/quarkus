package io.quarkus.reactive.mssql.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.mssqlclient.MSSQLPool;

public class DevServicesMsSQLDatasourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("container-license-acceptance.txt"))
            // Expect no warnings from reactive
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getMessage().toLowerCase(Locale.ENGLISH).contains("reactive"))
            .assertLogRecords(records -> assertThat(records)
                    // This is just to get meaningful error messages, as LogRecord doesn't have a toString()
                    .extracting(LogRecord::getMessage)
                    .isEmpty());

    @Inject
    MSSQLPool pool;

    @Test
    public void testDatasource() throws Exception {
        pool.withConnection(conn -> conn.query("SELECT 1").execute().replaceWithVoid())
                .await().atMost(Duration.ofMinutes(2));
    }
}
