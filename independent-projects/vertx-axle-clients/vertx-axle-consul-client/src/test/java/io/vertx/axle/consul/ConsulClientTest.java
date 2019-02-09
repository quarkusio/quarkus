package io.vertx.axle.consul;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsulClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("consul")
            .withExposedPorts(8500);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        ConsulClient client = ConsulClient.create(vertx, new ConsulClientOptions()
                .setHost(container.getContainerIpAddress())
                .setPort(container.getMappedPort(8500)));

        String uuid = UUID.randomUUID().toString();
        String stored = client.putValue("key", uuid)
                .thenCompose(x -> client.getValue("key"))
                .thenApply(KeyValue::getValue)
                .toCompletableFuture()
                .join();
        assertThat(stored).isEqualTo(uuid);
    }
}
