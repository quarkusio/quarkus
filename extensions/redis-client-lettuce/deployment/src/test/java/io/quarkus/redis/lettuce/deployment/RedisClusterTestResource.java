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
 * A Redis cluster of six nodes (three upstream nodes with one replica each) running in a single container, see
 * {@code redis/start-cluster.sh}, for the cluster tests. Exposes:
 * <ul>
 * <li>{@code redis.cluster.hosts}: the {@code redis://} URIs of the first three nodes, the seeds the clients discover
 * the topology from (a few nodes only, as the other three are to be discovered);</li>
 * <li>{@code redis.cluster.hosts-db1}: the same three URIs selecting database 1, which a cluster does not have;</li>
 * <li>{@code redis.cluster.all-hosts}: the URIs of all the nodes.</li>
 * </ul>
 * The nodes announce {@code 127.0.0.1} and a port that is the same inside the container and on the host, as a client
 * on the host follows the announced addresses ({@code MOVED} redirects, {@code CLUSTER SLOTS}) and the nodes
 * replicate from each other through them too. The ports are free host ports picked when the class is loaded (below
 * 55536, as the cluster bus port of a node is its port plus 10000), and published on the same port.
 * <p>
 * This resource is global, like the other ones of the module (see {@link RedisTlsTestResource} for why).
 */
public class RedisClusterTestResource implements QuarkusTestResourceLifecycleManager {

    public static final int NODES = 6;
    public static final int SEEDS = 3;

    static final List<Integer> PORTS = TestPorts.free(NODES);

    static final GenericContainer<?> CLUSTER = new RedisClusterContainer(PORTS)
            .withCopyFileToContainer(MountableFile.forClasspathResource("redis/start-cluster.sh", 0755),
                    "/start-cluster.sh")
            .withCommand(command())
            // the output of redis-cli --cluster create and of the nodes, to diagnose a cluster that does not come up
            .withLogConsumer(frame -> System.out.print("[redis-cluster] " + frame.getUtf8String()))
            .waitingFor(Wait.forLogMessage(".*CLUSTER READY.*\\n", 1).withStartupTimeout(Duration.ofSeconds(120)));

    /** A redis container publishing the given ports on the same host ports. */
    private static final class RedisClusterContainer extends GenericContainer<RedisClusterContainer> {

        RedisClusterContainer(List<Integer> ports) {
            super(DockerImageName.parse(System.getProperty("redis.base.image", "redis:8-alpine")));
            for (int port : ports) {
                addFixedExposedPort(port, port);
            }
        }
    }

    private static String[] command() {
        List<String> command = new ArrayList<>();
        command.add("sh");
        command.add("/start-cluster.sh");
        for (int port : PORTS) {
            command.add(Integer.toString(port));
        }
        return command.toArray(String[]::new);
    }

    @Override
    public Map<String, String> start() {
        CLUSTER.start();
        return Map.of(
                "redis.cluster.hosts", hosts(PORTS.subList(0, SEEDS)),
                "redis.cluster.hosts-db1", hosts(PORTS.subList(0, SEEDS)).replace(",", "/1,") + "/1",
                "redis.cluster.all-hosts", hosts(PORTS));
    }

    @Override
    public void stop() {
        CLUSTER.stop();
    }

    private static String hosts(List<Integer> ports) {
        return ports.stream().map(port -> "redis://127.0.0.1:" + port).collect(Collectors.joining(","));
    }
}
