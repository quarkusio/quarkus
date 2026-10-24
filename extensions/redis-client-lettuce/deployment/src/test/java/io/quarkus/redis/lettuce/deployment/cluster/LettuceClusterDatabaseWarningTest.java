package io.quarkus.redis.lettuce.deployment.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.deployment.RedisClusterTestResource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * A cluster only has database 0: Lettuce ignores the database a seed URI selects rather than failing, so the startup
 * warning reports it, and the data source works on database 0.
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterDatabaseWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.hosts-db1}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster")
            .setLogRecordPredicate(record -> LettuceRecorder.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LettuceClusterDatabaseWarningTest::message)
                    .singleElement().asString()
                    .contains("quarkus.redis.hosts (the database of a URI: a cluster only has database 0)"));

    @Inject
    RedisDataSource blocking;

    @Test
    void worksOnDatabaseZero() {
        blocking.value(String.class).set("database:key", "v");
        assertThat(blocking.value(String.class).get("database:key")).isEqualTo("v");
        // the pool resets nothing on a cluster connection, so scoped connections work too
        blocking.withConnection(ds -> assertThat(ds.value(String.class).get("database:key")).isEqualTo("v"));
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }
}
