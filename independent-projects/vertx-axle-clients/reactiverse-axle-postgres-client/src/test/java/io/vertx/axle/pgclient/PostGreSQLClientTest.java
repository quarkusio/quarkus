package io.vertx.axle.pgclient;

import io.reactiverse.axle.pgclient.PgClient;
import io.reactiverse.axle.pgclient.PgRowSet;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.axle.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class PostGreSQLClientTest {

    @Rule
    public PostgreSQLContainer container = new PostgreSQLContainer();

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
        PgPoolOptions options = new PgPoolOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword())
                .setMaxSize(5);

        PgClient client = PgClient.pool(vertx, options);

        PgRowSet join = client.preparedQuery("SELECT 1").toCompletableFuture().join();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }
}
