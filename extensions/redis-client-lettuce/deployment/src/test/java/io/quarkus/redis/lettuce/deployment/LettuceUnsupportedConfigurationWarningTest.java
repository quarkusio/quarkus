package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the Lettuce backend warns at startup about the configured properties it does not apply, each with
 * the reason, and does not report the ones it applies: the password and the TLS settings (including the client key
 * and certificate) are set and honoured, since the server requires a password and mutual TLS, the pool size is
 * applied to the connection pool, and the protocol version, the command queue bound, the reconnect interval and
 * the connection timeout are applied to the client, and the pool recycling properties to the connection pool, so
 * none of them must appear in the warning.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceUnsupportedConfigurationWarningTest {

    private static final String CERTS = RedisTlsTestResource.CERTS_DIR + "/" + RedisTlsTestResource.CERT_NAME;

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            // two URIs to the same server, only the first one is used
            .overrideConfigKey("quarkus.redis.hosts",
                    "rediss://${redis.mtls.host}:${redis.mtls.port}/0,rediss://${redis.mtls.host}:${redis.mtls.port}/1")
            .overrideConfigKey("quarkus.redis.password", "${redis.mtls.password}")
            .overrideConfigKey("quarkus.redis.tls.trust-certificate-pem.certs", CERTS + "-client-ca.crt")
            .overrideConfigKey("quarkus.redis.tls.key-certificate-pem", "true")
            .overrideConfigKey("quarkus.redis.tls.key-certificate-pem.certs", CERTS + "-client.crt")
            .overrideConfigKey("quarkus.redis.tls.key-certificate-pem.keys", CERTS + "-client.key")
            // replicas only applies to the cluster, Sentinel and replication client types
            .overrideConfigKey("quarkus.redis.replicas", "share")
            .overrideConfigKey("quarkus.redis.topology-cache-ttl", "2s")
            .overrideConfigKey("quarkus.redis.max-pool-size", "10")
            .overrideConfigKey("quarkus.redis.pool-recycle-timeout", "1m")
            .overrideConfigKey("quarkus.redis.pool-cleaner-interval", "10s")
            // applied to the Lettuce client, so not reported
            .overrideConfigKey("quarkus.redis.max-waiting-handlers", "4096")
            .overrideConfigKey("quarkus.redis.preferred-protocol-version", "resp2")
            .overrideConfigKey("quarkus.redis.reconnect-interval", "2s")
            .overrideConfigKey("quarkus.redis.tcp.connection-timeout", "5s")
            // no Lettuce equivalent, reported with the reason
            .overrideConfigKey("quarkus.redis.reconnect-attempts", "3")
            .overrideConfigKey("quarkus.redis.max-nested-arrays", "64")
            .overrideConfigKey("quarkus.redis.tcp.idle-timeout", "30s")
            // applied only together with configure-client-name, as with the Vert.x client
            .overrideConfigKey("quarkus.redis.client-name", "my-app")
            .setLogRecordPredicate(record -> LettuceRecorder.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LettuceUnsupportedConfigurationWarningTest::message)
                    .singleElement().asString()
                    .contains("Lettuce Redis client '<default>'")
                    .contains("quarkus.redis.hosts (only the first URI is used)")
                    .contains("quarkus.redis.replicas")
                    .contains("quarkus.redis.topology-cache-ttl")
                    .contains("quarkus.redis.reconnect-attempts (Lettuce reconnects a lost connection until it succeeds")
                    .contains("quarkus.redis.max-nested-arrays (Lettuce does not limit the nesting of replies)")
                    .contains("quarkus.redis.tcp.idle-timeout (Lettuce does not close idle connections)")
                    .contains("quarkus.redis.client-name (applied only with quarkus.redis.configure-client-name=true")
                    // honoured by the connection pool, so not reported
                    .doesNotContain("max-pool-size")
                    .doesNotContain("pool-recycle-timeout")
                    .doesNotContain("pool-cleaner-interval")
                    // honoured by the client, so not reported
                    .doesNotContain("max-waiting-handlers")
                    .doesNotContain("preferred-protocol-version")
                    .doesNotContain("reconnect-interval")
                    .doesNotContain("connection-timeout")
                    .doesNotContain("password")
                    .doesNotContain("tls"));

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Test
    void passwordAndTlsAreApplied() {
        assertThat(connection.sync().ping()).isEqualTo("PONG");
    }

    @Test
    void protocolVersionIsApplied() {
        assertThat(connection.sync().clientInfo()).contains(" resp=2 ");
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }
}
