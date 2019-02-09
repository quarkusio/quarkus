package io.vertx.axle.cassandra;

import io.vertx.axle.core.Vertx;
import io.vertx.cassandra.CassandraClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class CassandraClientTest {

    @Rule
    public GenericContainer container = new CassandraContainer()
            .withExposedPorts(9042);

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
        CassandraClient client = CassandraClient.createNonShared(vertx, new CassandraClientOptions()
                .addContactPoint(container.getContainerIpAddress())
                .setPort(container.getMappedPort(9042)));

        client.connect()
                .toCompletableFuture().join();
    }
}
