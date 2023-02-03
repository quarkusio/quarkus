package io.quarkus.reactive.oracle.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.oracleclient.OraclePool;

public class DevServicesOracleDatasourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withEmptyApplication()
            // Expect no warnings from reactive
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getMessage().toLowerCase(Locale.ENGLISH).contains("reactive"))
            .assertLogRecords(records -> assertThat(records)
                    // This is just to get meaningful error messages, as LogRecord doesn't have a toString()
                    .extracting(LogRecord::getMessage)
                    .isEmpty());

    @Inject
    OraclePool pool;

    @Test
    public void testDatasource() throws Exception {
        pool.withConnection(conn -> conn.query("SELECT 1 FROM DUAL").execute().replaceWithVoid())
                .await().atMost(Duration.ofMinutes(2));
    }
}
