package io.quarkus.hibernate.search.orm.outboxpolling.runtime;

import java.util.Optional;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.UuidGenerationStrategy;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface HibernateSearchOutboxPollingBuildTimeConfigPersistenceUnit {

    /**
     * Configuration for coordination between threads or application instances.
     */
    CoordinationConfig coordination();

    @ConfigGroup
    interface CoordinationConfig {

        /**
         * Configuration for the mapping of entities used for outbox-polling coordination.
         */
        @ConfigDocSection
        EntityMappingConfig entityMapping();

    }

    @ConfigGroup
    interface EntityMappingConfig {

        /**
         * Configuration for the "agent" entity mapping.
         */
        EntityMappingAgentConfig agent();

        /**
         * Configuration for the "outbox event" entity mapping.
         */
        EntityMappingOutboxEventConfig outboxEvent();

    }

    @ConfigGroup
    interface EntityMappingAgentConfig {

        /**
         * The database catalog to use for the agent table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("Default catalog configured in Hibernate ORM")
        Optional<String> catalog();

        /**
         * The schema catalog to use for the agent table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("Default catalog configured in Hibernate ORM")
        Optional<String> schema();

        /**
         * The name of the agent table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("HSEARCH_AGENT")
        Optional<String> table();

        /**
         * The UUID generator strategy used for the agent table.
         *
         * Available strategies:
         *
         * * `auto` (the default) is the same as `random` which uses `UUID#randomUUID()`.
         * * `time` is an IP based strategy consistent with IETF RFC 4122.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("auto")
        Optional<UuidGenerationStrategy> uuidGenStrategy();

        /**
         * The name of the Hibernate ORM basic type used for representing an UUID in the outbox event table.
         *
         * Refer to
         * link:{hibernate-orm-docs-url}#basic-uuid[this section of the Hibernate ORM documentation]
         * to see the possible UUID representations.
         *
         * Defaults to the special value `default`, which will result into one of `char`/`binary`
         * depending on the database kind.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("char/binary depending on the database kind")
        Optional<String> uuidType();

    }

    @ConfigGroup
    interface EntityMappingOutboxEventConfig {

        /**
         * The database catalog to use for the outbox event table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("Default catalog configured in Hibernate ORM")
        Optional<String> catalog();

        /**
         * The schema catalog to use for the outbox event table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("Default schema configured in Hibernate ORM")
        Optional<String> schema();

        /**
         * The name of the outbox event table.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("HSEARCH_OUTBOX_EVENT")
        Optional<String> table();

        /**
         * The UUID generator strategy used for the outbox event table.
         *
         * Available strategies:
         *
         * * `auto` (the default) is the same as `random` which uses `UUID#randomUUID()`.
         * * `time` is an IP based strategy consistent with IETF RFC 4122.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("auto")
        Optional<UuidGenerationStrategy> uuidGenStrategy();

        /**
         * The name of the Hibernate ORM basic type used for representing an UUID in the outbox event table.
         *
         * Refer to
         * link:{hibernate-orm-docs-url}#basic-uuid[this section of the Hibernate ORM documentation]
         * to see the possible UUID representations.
         *
         * Defaults to the special value `default`, which will result into one of `char`/`binary`
         * depending on the database kind.
         *
         * @asciidoclet
         */
        @ConfigDocDefault("char/binary depending on the database kind")
        Optional<String> uuidType();

    }

}
