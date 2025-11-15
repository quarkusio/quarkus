package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.elasticsearch")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ElasticsearchConfig {

    /**
     * The list of hosts of the Elasticsearch servers.
     */
    @WithDefault("localhost:9200")
    List<InetSocketAddress> hosts();

    /**
     * The protocol to use when contacting Elasticsearch servers.
     * Set to "https" to enable SSL/TLS.
     */
    @WithDefault("http")
    String protocol();

    /**
     * The username for basic HTTP authentication.
     */
    Optional<String> username();

    /**
     * The password for basic HTTP authentication.
     */
    Optional<String> password();

    /**
     * The connection timeout.
     */
    @WithDefault("1S")
    Duration connectionTimeout();

    /**
     * The socket timeout.
     */
    @WithDefault("30S")
    Duration socketTimeout();

    /**
     * The maximum number of connections to all the Elasticsearch servers.
     */
    @WithDefault("40")
    int maxConnections();

    /**
     * The maximum number of connections per Elasticsearch server.
     */
    @WithDefault("20")
    int maxConnectionsPerRoute();

    /**
     * The number of IO thread.
     * By default, this is the number of locally detected processors.
     * <p>
     * Thread counts higher than the number of processors should not be necessary because the I/O threads rely on non-blocking
     * operations, but you may want to use a thread count lower than the number of processors.
     *
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/_number_of_threads.html">
     *      number of threads</a>
     */
    Optional<Integer> ioThreadCounts();

    /**
     * Configuration for the automatic discovery of new Elasticsearch nodes.
     */
    DiscoveryConfig discovery();

    @ConfigGroup
    interface DiscoveryConfig {

        /**
         * Defines if automatic discovery is enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Refresh interval of the node list.
         */
        @WithDefault("5M")
        Duration refreshInterval();
    }
}
