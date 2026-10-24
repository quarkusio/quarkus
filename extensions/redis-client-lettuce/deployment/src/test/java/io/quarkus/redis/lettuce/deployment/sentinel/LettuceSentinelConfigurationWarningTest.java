package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Sentinel and replication properties the Lettuce backend applies are not reported in the startup warning:
 * for the Sentinel client {@code client-type}, the {@code hosts}, {@code master-name}, {@code role},
 * {@code auto-failover} and {@code replicas}; for the replication client {@code topology} and {@code replicas}.
 * The sentinel-only {@code master-name} is reported for the replication client, and {@code cluster-transactions}
 * for both. The one Sentinel property that is set aside is reported too: {@code replicas} when {@code role=replica}
 * already sends the reads to the replicas.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceSentinelConfigurationWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}")
            .overrideConfigKey("quarkus.redis.role", "master")
            .overrideConfigKey("quarkus.redis.auto-failover", "true")
            .overrideConfigKey("quarkus.redis.replicas", "share")
            .overrideConfigKey("quarkus.redis.cluster-transactions", "single-node")
            .overrideConfigKey("quarkus.redis.nodes.hosts", "${redis.replication.hosts}")
            .overrideConfigKey("quarkus.redis.nodes.client-type", "replication")
            .overrideConfigKey("quarkus.redis.nodes.topology", "static")
            .overrideConfigKey("quarkus.redis.nodes.replicas", "share")
            .overrideConfigKey("quarkus.redis.nodes.master-name", "not-a-sentinel-client")
            .overrideConfigKey("quarkus.redis.reader.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.reader.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.reader.master-name", "${redis.sentinel.master-name}")
            .overrideConfigKey("quarkus.redis.reader.role", "replica")
            .overrideConfigKey("quarkus.redis.reader.replicas", "never")
            .setLogRecordPredicate(record -> LettuceRecorder.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> {
                assertThat(records).extracting(LettuceSentinelConfigurationWarningTest::message)
                        .filteredOn(message -> message.contains("'<default>'"))
                        .singleElement().asString()
                        .contains("quarkus.redis.cluster-transactions")
                        .doesNotContain("client-type")
                        .doesNotContain("hosts")
                        .doesNotContain("master-name")
                        .doesNotContain("role")
                        .doesNotContain("auto-failover")
                        .doesNotContain("replicas");
                assertThat(records).extracting(LettuceSentinelConfigurationWarningTest::message)
                        .filteredOn(message -> message.contains("'nodes'"))
                        .singleElement().asString()
                        .contains("quarkus.redis.nodes.master-name")
                        .doesNotContain("topology")
                        .doesNotContain("replicas")
                        .doesNotContain("hosts");
                // role=replica decides where the reads go: a replicas setting next to it is not applied, and says so
                assertThat(records).extracting(LettuceSentinelConfigurationWarningTest::message)
                        .filteredOn(message -> message.contains("'reader'"))
                        .singleElement().asString()
                        .contains("quarkus.redis.reader.replicas (role=replica reads from the replicas)")
                        .doesNotContain("quarkus.redis.reader.role")
                        .doesNotContain("master-name")
                        .doesNotContain("hosts");
            });

    @Inject
    RedisDataSource sentinel;

    @Inject
    @RedisClientName("nodes")
    RedisDataSource replication;

    @Inject
    @RedisClientName("reader")
    RedisDataSource reader;

    @Test
    void propertiesAreApplied() {
        assertThat(sentinel.execute("PING").toString()).isEqualTo("PONG");
        assertThat(replication.execute("PING").toString()).isEqualTo("PONG");
        assertThat(reader.execute("PING").toString()).isEqualTo("PONG");
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }
}
