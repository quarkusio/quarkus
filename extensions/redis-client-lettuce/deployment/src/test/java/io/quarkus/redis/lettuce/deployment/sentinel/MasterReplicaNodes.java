package io.quarkus.redis.lettuce.deployment.sentinel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * Test-side view of the Sentinel deployment of {@link io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource}:
 * plain connections to the data nodes and to a sentinel, to look at the roles, the served commands and to trigger a
 * failover, independently of the client under test.
 */
final class MasterReplicaNodes implements AutoCloseable {

    private static final Pattern GET_CALLS = Pattern.compile("cmdstat_get:calls=(\\d+)");

    private final RedisClient client = RedisClient.create();
    private final List<Integer> ports = new ArrayList<>();
    private final List<StatefulRedisConnection<String, String>> nodes = new ArrayList<>();
    private final StatefulRedisSentinelConnection<String, String> sentinel;
    private final String masterName;

    /**
     * @param replicationHosts the {@code redis.replication.hosts} property (the data nodes)
     * @param sentinelHosts the {@code redis.sentinel.hosts} property; the first sentinel is used
     * @param masterName the {@code redis.sentinel.master-name} property
     */
    MasterReplicaNodes(String replicationHosts, String sentinelHosts, String masterName) {
        for (String host : replicationHosts.split(",")) {
            RedisURI uri = RedisURI.create(URI.create(host));
            ports.add(uri.getPort());
            nodes.add(client.connect(uri));
        }
        this.sentinel = client.connectSentinel(RedisURI.create(URI.create(sentinelHosts.split(",")[0])));
        this.masterName = masterName;
    }

    List<Integer> ports() {
        return ports;
    }

    /** The port of the master as the sentinel currently reports it. */
    int masterPort() {
        SocketAddress address = sentinel.sync().getMasterAddrByName(masterName);
        return ((InetSocketAddress) address).getPort();
    }

    /** Asks the sentinel to fail over to the replica. */
    String failover() {
        return sentinel.sync().failover(masterName);
    }

    /** The {@code role:} line of {@code INFO replication} of the node on {@code port}. */
    String role(int port) {
        return info(port, "replication").lines().filter(line -> line.startsWith("role:")).findFirst().orElseThrow()
                .substring("role:".length()).trim();
    }

    /** Whether the node on {@code port} is a replica whose link to its master is up. */
    boolean isSyncedReplica(int port) {
        String info = info(port, "replication");
        return info.contains("role:slave") && info.contains("master_link_status:up");
    }

    /** The number of {@code GET}s the node on {@code port} has served so far. */
    long gets(int port) {
        Matcher matcher = GET_CALLS.matcher(info(port, "commandstats"));
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
    }

    private String info(int port, String section) {
        return nodes.get(ports.indexOf(port)).sync().info(section);
    }

    @Override
    public void close() {
        for (StatefulRedisConnection<String, String> node : nodes) {
            node.close();
        }
        sentinel.close();
        client.shutdown();
    }
}
