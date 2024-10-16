package io.quarkus.kafka.client.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Allows configuring the Redpanda broker.
 * Notice that Redpanda is not a "genuine" Kafka, it's a 100% compatible implementation of the protocol.
 *
 * Find more info about Redpanda on <a href="https://redpanda.com/">https://redpanda.com/</a>.
 */
@ConfigGroup
public class RedpandaBuildTimeConfig {

    /**
     * Enables transaction support.
     * Also enables the producer idempotence.
     *
     * Find more info about Redpanda transaction support on
     * <a href="https://vectorized.io/blog/fast-transactions/">https://vectorized.io/blog/fast-transactions/</a>.
     *
     * Notice that
     * <a href=
     * "https://cwiki.apache.org/confluence/display/KAFKA/KIP-447%3A+Producer+scalability+for+exactly+once+semantics">KIP-447
     * (producer scalability for exactly once semantic)</a> and
     * <a href="https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=89068820">KIP-360 (Improve reliability of
     * idempotent/transactional producer)</a> are <em>not</em> supported.
     */
    @ConfigItem(defaultValue = "true")
    public boolean transactionEnabled;

    /**
     * Port to access the Redpanda HTTP Proxy (<a href="https://docs.redpanda.com/current/develop/http-proxy/">pandaproxy</a>).
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> proxyPort;
}
