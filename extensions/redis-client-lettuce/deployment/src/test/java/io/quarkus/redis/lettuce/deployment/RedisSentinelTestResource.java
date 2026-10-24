package io.quarkus.redis.lettuce.deployment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A Redis Sentinel deployment running in a single container, see {@code redis/start-sentinel.sh}: one
 * master, one replica and three sentinels monitoring the master as {@value #MASTER_NAME}. It doubles as the
 * replication deployment (the master and its replica) of the replication client tests. Exposes:
 * <ul>
 * <li>{@code redis.sentinel.hosts}: the {@code redis://} URIs of the three sentinels;</li>
 * <li>{@code redis.sentinel.master-name}: {@value #MASTER_NAME};</li>
 * <li>{@code redis.replication.hosts}: the URIs of the master and the replica, in that order at startup (a
 * failover test may swap their roles; the clients ask the nodes for their roles);</li>
 * <li>{@code redis.replication.reversed-hosts}: the same two URIs, replica first;</li>
 * <li>{@code redis.replication.master}: the URI of the node that is the master at startup;</li>
 * <li>{@code redis.replication.replica}: the URI of the node that is the replica at startup.</li>
 * </ul>
 * Like the cluster, every process announces {@code 127.0.0.1} and a port that is the same inside the container and
 * on the host, as the sentinels hand those addresses to the clients and the replica uses them too. The ports are
 * free host ports picked when the class is loaded and published on the same port.
 * <p>
 * This resource is global, like the other ones of the module (see {@link RedisTlsTestResource} for why).
 */
public class RedisSentinelTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String MASTER_NAME = "mymaster";
    public static final int SENTINELS = 3;

    /** Master, replica, then the sentinels. */
    static final List<Integer> PORTS = TestPorts.free(2 + SENTINELS);

    static final GenericContainer<?> SENTINEL = new RedisSentinelContainer(PORTS)
            .withCopyFileToContainer(MountableFile.forClasspathResource("redis/start-sentinel.sh", 0755),
                    "/start-sentinel.sh")
            .withEnv("MASTER_NAME", MASTER_NAME)
            .withCommand(command())
            // the output of the start script, to diagnose a deployment that does not come up
            .withLogConsumer(frame -> System.out.print("[redis-sentinel] " + frame.getUtf8String()))
            .waitingFor(Wait.forLogMessage(".*SENTINEL READY.*\\n", 1).withStartupTimeout(Duration.ofSeconds(120)));

    /** A redis container publishing the given ports on the same host ports. */
    private static final class RedisSentinelContainer extends GenericContainer<RedisSentinelContainer> {

        RedisSentinelContainer(List<Integer> ports) {
            super(DockerImageName.parse(System.getProperty("redis.base.image", "redis:8-alpine")));
            for (int port : ports) {
                addFixedExposedPort(port, port);
            }
        }
    }

    private static String[] command() {
        List<String> command = new ArrayList<>();
        command.add("sh");
        command.add("/start-sentinel.sh");
        for (int port : PORTS) {
            command.add(Integer.toString(port));
        }
        return command.toArray(String[]::new);
    }

    @Override
    public Map<String, String> start() {
        SENTINEL.start();
        return Map.of(
                "redis.sentinel.hosts", hosts(PORTS.subList(2, PORTS.size())),
                "redis.sentinel.master-name", MASTER_NAME,
                "redis.replication.hosts", hosts(PORTS.subList(0, 2)),
                "redis.replication.reversed-hosts", hosts(List.of(PORTS.get(1), PORTS.get(0))),
                "redis.replication.master", hosts(PORTS.subList(0, 1)),
                "redis.replication.replica", hosts(PORTS.subList(1, 2)));
    }

    @Override
    public void stop() {
        SENTINEL.stop();
    }

    private static String hosts(List<Integer> ports) {
        return ports.stream().map(port -> "redis://127.0.0.1:" + port).collect(Collectors.joining(","));
    }
}
