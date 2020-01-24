package io.quarkus.hazelcast.client.runtime;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "hazelcast-client", phase = ConfigPhase.RUN_TIME)
public class HazelcastClientConfig {

    /**
     * Hazelcast Cluster members
     */
    @ConfigItem
    public Optional<List<InetSocketAddress>> clusterMembers;

    /**
     * Hazelcast client labels
     */
    @ConfigItem
    public Optional<List<String>> labels;

    /**
     * Hazelcast Cluster group name
     */
    @ConfigItem
    public Optional<String> clusterName;

    /**
     * Outbound ports
     */
    @ConfigItem
    public Optional<List<Integer>> outboundPorts;

    /**
     * Outbound port definitions
     */
    @ConfigItem
    public Optional<List<String>> outboundPortDefinitions;

    /**
     * Connection timeout
     */
    @ConfigItem
    public OptionalInt connectionTimeout;
}
