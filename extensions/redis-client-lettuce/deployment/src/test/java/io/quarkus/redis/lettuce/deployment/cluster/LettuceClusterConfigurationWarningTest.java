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
 * The cluster properties the Lettuce backend applies ({@code client-type}, all the {@code hosts}, {@code replicas},
 * {@code topology-cache-ttl}) are not reported in the startup warning about the configuration it does not apply,
 * whereas {@code cluster-transactions}, which it does not support, is.
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterConfigurationWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.all-hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster")
            .overrideConfigKey("quarkus.redis.replicas", "share")
            .overrideConfigKey("quarkus.redis.topology-cache-ttl", "2s")
            .overrideConfigKey("quarkus.redis.cluster-transactions", "single-node")
            .setLogRecordPredicate(record -> LettuceRecorder.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LettuceClusterConfigurationWarningTest::message)
                    .singleElement().asString()
                    .contains("quarkus.redis.cluster-transactions")
                    .doesNotContain("client-type")
                    .doesNotContain("hosts")
                    .doesNotContain("replicas")
                    .doesNotContain("topology-cache-ttl"));

    @Inject
    RedisDataSource blocking;

    @Test
    void clusterPropertiesAreApplied() {
        assertThat(blocking.execute("PING").toString()).isEqualTo("PONG");
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }
}
