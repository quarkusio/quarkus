/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hibernate-search", phase = ConfigPhase.RUN_TIME)
public class HibernateSearchElasticsearchRuntimeConfig {

    /**
     * Configuration of the default backend.
     */
    ElasticsearchBackendRuntimeConfig elasticsearch;

    /**
     * Configuration of optional additional backends.
     */
    @ConfigItem(name = "elasticsearch.backends")
    Map<String, ElasticsearchBackendRuntimeConfig> additionalBackends;

    @ConfigGroup
    public static class ElasticsearchBackendRuntimeConfig {
        /**
         * The list of hosts of the Elasticsearch servers.
         */
        @ConfigItem
        List<String> hosts;

        /**
         * The username used for authentication.
         */
        @ConfigItem
        Optional<String> username;

        /**
         * The password used for authentication.
         */
        @ConfigItem
        Optional<String> password;

        /**
         * The connection timeout.
         */
        @ConfigItem
        Optional<Duration> connectionTimeout;

        /**
         * The maximum number of connections to all the Elasticsearch servers.
         */
        @ConfigItem
        OptionalInt maxConnections;

        /**
         * The maximum number of connections per Elasticsearch server.
         */
        @ConfigItem
        OptionalInt maxConnectionsPerRoute;

        /**
         * Configuration for the automatic discovery of new Elasticsearch nodes.
         */
        @ConfigItem
        DiscoveryConfig discovery;

        /**
         * Configuration for the automatic indexing.
         */
        @ConfigItem
        AutomaticIndexing automaticIndexing;

        /**
         * The default configuration for the Elasticsearch indexes.
         */
        @ConfigItem
        ElasticsearchIndexConfig indexDefaults;

        /**
         * Per-index specific configuration.
         */
        @ConfigItem
        Map<String, ElasticsearchIndexConfig> indexes;
    }

    @ConfigGroup
    public static class ElasticsearchIndexConfig {
        /**
         * Configuration for the lifecyle of the indexes.
         */
        @ConfigItem
        LifecycleConfig lifecycle;

        /**
         * Defines if the indexes should be refreshed after writes.
         */
        @ConfigItem
        Optional<Boolean> refreshAfterWrite;
    }

    @ConfigGroup
    public static class DiscoveryConfig {

        /**
         * Defines if automatic discovery is enabled.
         */
        @ConfigItem
        Optional<Boolean> enabled;

        /**
         * Refresh interval of the node list.
         */
        Optional<Duration> refreshInterval;

        /**
         * The scheme that should be used for the new nodes discovered.
         */
        Optional<String> defaultScheme;
    }

    @ConfigGroup
    public static class AutomaticIndexing {

        /**
         * The synchronization strategy to use when indexing automatically.
         * <p>
         * Defines the status for which you wait before considering the operation completed by Hibernate Search.
         * <p>
         * Can be either one of "queued", "committed" or "searchable".
         * <p>
         * Using "searchable" is recommend in unit tests.
         */
        Optional<HibernateOrmAutomaticIndexingSynchronizationStrategyName> synchronizationStrategy;

        /**
         * Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.
         * <p>
         * When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when
         * indexing.
         */
        Optional<Boolean> enableDirtyCheck;
    }

    @ConfigGroup
    public static class LifecycleConfig {

        /**
         * The strategy used for index lifecycle.
         * <p>
         * Must be one of: none, validate, update, create, drop-and-create or drop-and-create-and-drop.
         */
        @ConfigItem
        Optional<ElasticsearchIndexLifecycleStrategyName> strategy;

        /**
         * The minimal cluster status required.
         * <p>
         * Must be one of: green, yellow, red.
         */
        @ConfigItem
        Optional<ElasticsearchIndexStatus> requiredStatus;

        /**
         * How long we should wait for the status before failing the bootstrap.
         */
        @ConfigItem
        Optional<Duration> requiredStatusWaitTimeout;
    }
}
