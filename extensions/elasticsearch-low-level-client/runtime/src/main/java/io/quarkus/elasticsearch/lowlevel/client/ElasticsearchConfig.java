package io.quarkus.elasticsearch.lowlevel.client;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ElasticsearchConfig {
    /**
     * The list of hosts of the Elasticsearch servers.
     */
    @ConfigItem(defaultValue = "localhost:9200")
    public List<String> hosts;

    /**
     * The protocol to use when contacting Elasticsearch servers.
     * Set to "https" to enable SSL/TLS.
     */
    @ConfigItem(defaultValue = "http")
    public String protocol;

    /**
     * The username used for authentication.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password used for authentication.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The connection timeout.
     */
    @ConfigItem(defaultValue = "1S")
    public Duration connectionTimeout;

    /**
     * The socket timeout.
     */
    @ConfigItem(defaultValue = "30S")
    public Duration socketTimeout;

    /**
     * The number of IO thread.
     * By default, this is the number of locally detected processors.
     * 
     * @link{https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/_number_of_threads.html}
     */
    @ConfigItem
    public Optional<Integer> ioThreadCounts;

    /**
     * Configuration for the automatic discovery of new Elasticsearch nodes.
     */
    @ConfigItem
    public DiscoveryConfig discovery;

    @ConfigGroup
    public static class DiscoveryConfig {

        /**
         * Defines if automatic discovery is enabled.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * Refresh interval of the node list.
         */
        @ConfigItem(defaultValue = "10S")
        public Duration refreshInterval;
    }
}
