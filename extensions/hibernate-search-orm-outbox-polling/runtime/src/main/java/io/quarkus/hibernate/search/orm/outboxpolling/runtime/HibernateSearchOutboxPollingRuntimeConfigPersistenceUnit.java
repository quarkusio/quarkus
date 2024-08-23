package io.quarkus.hibernate.search.orm.outboxpolling.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface HibernateSearchOutboxPollingRuntimeConfigPersistenceUnit {

    /**
     * Configuration for coordination between threads or application instances.
     */
    CoordinationConfig coordination();

    @ConfigGroup
    interface CoordinationConfig {

        /**
         * Default configuration.
         */
        @WithParentName
        AgentsConfig defaults();

        /**
         * Per-tenant configuration overrides.
         */
        @ConfigDocSection
        @ConfigDocMapKey("tenant-id")
        Map<String, AgentsConfig> tenants();

    }

    @ConfigGroup
    interface AgentsConfig {

        /**
         * Configuration for the event processor agent.
         */
        EventProcessorConfig eventProcessor();

        /**
         * Configuration for the mass indexer agent.
         */
        MassIndexerConfig massIndexer();

    }

    @ConfigGroup
    interface EventProcessorConfig {

        // @formatter:off
        /**
         * Whether the event processor is enabled,
         * i.e. whether events will be processed to perform automatic reindexing on this instance of the application.
         *
         * This can be set to `false` to disable event processing on some application nodes,
         * for example to dedicate some nodes to HTTP request processing and other nodes to event processing.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("true")
        boolean enabled();

        /**
         * Configuration related to shards.
         */
        EventProcessorShardsConfig shards();

        // @formatter:off
        /**
         * How long to wait for another query to the outbox events table after a query didn’t return any event.
         *
         * Lower values will reduce the time it takes for a change to be reflected in the index,
         * but will increase the stress on the database when there are no new events.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("0.100S")
        Duration pollingInterval();

        // @formatter:off
        /**
         * How long the event processor can poll for events before it must perform a "pulse",
         * updating and checking registrations in the agents table.
         *
         * The pulse interval must be set to a value between the polling interval
         * and one third (1/3) of the expiration interval.
         *
         * Low values (closer to the polling interval) mean less time wasted not processing events
         * when a node joins or leaves the cluster,
         * and reduced risk of wasting time not processing events
         * because an event processor is incorrectly considered disconnected,
         * but more stress on the database because of more frequent checks of the list of agents.
         *
         * High values (closer to the expiration interval) mean more time wasted not processing events
         * when a node joins or leaves the cluster,
         * and increased risk of wasting time not processing events
         * because an event processor is incorrectly considered disconnected,
         * but less stress on the database because of less frequent checks of the list of agents.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("2S")
        Duration pulseInterval();

        // @formatter:off
        /**
         * How long an event processor "pulse" remains valid before considering the processor disconnected
         * and forcibly removing it from the cluster.
         *
         * The expiration interval must be set to a value at least 3 times larger than the pulse interval.
         *
         * Low values (closer to the pulse interval) mean less time wasted not processing events
         * when a node abruptly leaves the cluster due to a crash or network failure,
         * but increased risk of wasting time not processing events
         * because an event processor is incorrectly considered disconnected.
         *
         * High values (much larger than the pulse interval) mean more time wasted not processing events
         * when a node abruptly leaves the cluster due to a crash or network failure,
         * but reduced risk of wasting time not processing events
         * because an event processor is incorrectly considered disconnected.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("30S")
        Duration pulseExpiration();

        // @formatter:off
        /**
         * How many outbox events, at most, are processed in a single transaction.
         *
         * Higher values will reduce the number of transactions opened by the background process
         * and may increase performance thanks to the first-level cache (persistence context),
         * but will increase memory usage and in extreme cases may lead to ``OutOfMemoryError``s.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("50")
        int batchSize();

        // @formatter:off
        /**
         * The timeout for transactions processing outbox events.
         *
         * When this property is not set,
         * Hibernate Search will use whatever default transaction timeout is configured in the JTA transaction manager,
         * which may be too low for batch processing and lead to transaction timeouts when processing batches of events.
         * If this happens, set a higher transaction timeout for event processing using this property.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        Optional<Duration> transactionTimeout();

        // @formatter:off
        /**
         * How long the event processor must wait before re-processing an event after its previous processing failed.
         *
         * Use the value `0S` to reprocess failed events as soon as possible, with no delay.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("30S")
        Duration retryDelay();

    }

    @ConfigGroup
    interface EventProcessorShardsConfig {

        // @formatter:off
        /**
         * The total number of shards that will form a partition of the entity change events to process.
         *
         * By default, sharding is dynamic and setting this property is not necessary.
         *
         * If you want to control explicitly the number and assignment of shards,
         * you must configure static sharding and then setting this property as well as the assigned shards (see `shards.assigned`)
         * is necessary.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor-sharding[this section of the reference documentation]
         * for more information about event processor sharding.
         *
         * @asciidoclet
         */
        // @formatter:on
        OptionalInt totalCount();

        // @formatter:off
        /**
         * Among shards that will form a partition of the entity change events,
         * the shards that will be processed by this application instance.
         *
         * By default, sharding is dynamic and setting this property is not necessary.
         *
         * If you want to control explicitly the number and assignment of shards,
         * you must configure static sharding and then setting this property as well as the total shard count
         * is necessary.
         *
         * Shards are referred to by an index in the range `[0, total_count - 1]` (see `shards.total-count`).
         * A given application node must be assigned at least one shard but may be assigned multiple shards
         * by setting `shards.assigned` to a comma-separated list, e.g. `0,3`.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-event-processor-sharding[this section of the reference documentation]
         * for more information about event processor sharding.
         *
         * @asciidoclet
         */
        // @formatter:on
        Optional<List<Integer>> assigned();

    }

    @ConfigGroup
    interface MassIndexerConfig {

        // @formatter:off
        /**
         * How long to wait for another query to the agent table
         * when actively waiting for event processors to suspend themselves.
         *
         * Low values will reduce the time it takes for the mass indexer agent to detect
         * that event processors finally suspended themselves,
         * but will increase the stress on the database while the mass indexer agent is actively waiting.
         *
         * High values will increase the time it takes for the mass indexer agent to detect
         * that event processors finally suspended themselves,
         * but will reduce the stress on the database while the mass indexer agent is actively waiting.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-mass-indexer[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("0.100S")
        Duration pollingInterval();

        // @formatter:off
        /**
         * How long the mass indexer can wait before it must perform a "pulse",
         * updating and checking registrations in the agent table.
         *
         * The pulse interval must be set to a value between the polling interval
         * and one third (1/3) of the expiration interval.
         *
         * Low values (closer to the polling interval) mean reduced risk of
         * event processors starting to process events again during mass indexing
         * because a mass indexer agent is incorrectly considered disconnected,
         * but more stress on the database because of more frequent updates of the mass indexer agent's entry in the agent table.
         *
         * High values (closer to the expiration interval) mean increased risk of
         * event processors starting to process events again during mass indexing
         * because a mass indexer agent is incorrectly considered disconnected,
         * but less stress on the database because of less frequent updates of the mass indexer agent's entry in the agent table.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-mass-indexer[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("2S")
        Duration pulseInterval();

        // @formatter:off
        /**
         * How long an event processor "pulse" remains valid before considering the processor disconnected
         * and forcibly removing it from the cluster.
         *
         * The expiration interval must be set to a value at least 3 times larger than the pulse interval.
         *
         * Low values (closer to the pulse interval) mean less time wasted with event processors not processing events
         * when a mass indexer agent terminates due to a crash,
         * but increased risk of event processors starting to process events again during mass indexing
         * because a mass indexer agent is incorrectly considered disconnected.
         *
         * High values (much larger than the pulse interval) mean more time wasted with event processors not processing events
         * when a mass indexer agent terminates due to a crash,
         * but reduced risk of event processors starting to process events again during mass indexing
         * because a mass indexer agent is incorrectly considered disconnected.
         *
         * See
         * link:{hibernate-search-docs-url}#coordination-outbox-polling-mass-indexer[this section of the reference documentation]
         * for more information.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithDefault("30S")
        Duration pulseExpiration();

    }

}
