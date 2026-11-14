package io.quarkus.it.ratelimit;

import java.time.Duration;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A three-shard Redis Cluster without replicas in one container on the host network, so that the addresses the nodes
 * announce ({@code 127.0.0.1:<port>}) are the ones the client reaches.
 */
public class RedisClusterResource implements QuarkusTestResourceLifecycleManager {

    static final int[] PORTS = { 7379, 7380, 7381 };

    private GenericContainer<?> cluster;

    @Override
    public Map<String, String> start() {
        StringBuilder script = new StringBuilder();
        StringBuilder nodes = new StringBuilder();
        for (int port : PORTS) {
            script.append("redis-server --port ").append(port).append(" --cluster-enabled yes --cluster-config-file nodes-")
                    .append(port).append(".conf --save '' --appendonly no --daemonize yes && ");
            nodes.append(" 127.0.0.1:").append(port);
        }
        for (int port : PORTS) {
            script.append("until redis-cli -p ").append(port).append(" ping; do sleep 0.1; done && ");
        }
        script.append("redis-cli --cluster create").append(nodes).append(" --cluster-replicas 0 --cluster-yes && ");
        for (int port : PORTS) {
            script.append("until redis-cli -p ").append(port)
                    .append(" cluster info | grep -q cluster_state:ok; do sleep 0.1; done && ");
        }
        script.append("echo CLUSTER READY && tail -f /dev/null");
        // the image of the Redis Dev Services default (redis.image in build-parent/pom.xml); host networking, so that
        // the nodes announce addresses the client can reach, requires a Linux host
        cluster = new GenericContainer<>(DockerImageName.parse("docker.io/library/redis:8"))
                .withNetworkMode("host")
                .withCommand("sh", "-c", script.toString())
                .waitingFor(Wait.forLogMessage(".*CLUSTER READY.*", 1).withStartupTimeout(Duration.ofMinutes(1)));
        cluster.start();
        return Map.of("quarkus.redis.hosts", "redis://127.0.0.1:" + PORTS[0],
                "quarkus.redis.client-type", "cluster");
    }

    @Override
    public void stop() {
        if (cluster != null) {
            cluster.stop();
        }
    }
}
