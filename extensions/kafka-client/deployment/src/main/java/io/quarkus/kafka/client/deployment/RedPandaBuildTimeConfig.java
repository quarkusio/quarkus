package io.quarkus.kafka.client.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Allows configuring the Red Panda broker.
 * Notice that Red Panda is not a "genuine" Kafka, it's a 100% compatible implementation of the protocol.
 *
 * Find more info about Red Panda on <a href="https://vectorized.io/redpanda/">https://vectorized.io/redpanda/</a>.
 */
@ConfigGroup
public class RedPandaBuildTimeConfig {

    /**
     * Enables transaction support.
     * Also enables the producer idempotence.
     *
     * Find more info about Red Panda transaction support on
     * <a href="https://vectorized.io/blog/fast-transactions/">https://vectorized.io/blog/fast-transactions/</a>.
     *
     * Notice that
     * <a href=
     * "https://cwiki.apache.org/confluence/display/KAFKA/KIP-447%3A+Producer+scalability+for+exactly+once+semantics">KIP-447
     * (producer scalability for exactly once semantic)</a> and
     * <a href="https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=89068820">KIP-360 (Improve reliability of
     * idempotent/transactional producer)</a> are <em>not</em> supported.
     */
    @ConfigItem(defaultValue = "false")
    public boolean transactionEnabled;
}
