package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.function.BiConsumer;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.health.MongoHealthCheck;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultMongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("default-mongoclient.properties");

    @Inject
    MongoClient client;

    @Inject
    ReactiveMongoClient reactiveClient;

    @Inject
    @Any
    MongoHealthCheck health;

    @AfterEach
    void cleanup() {
        if (reactiveClient != null) {
            reactiveClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testClientInjection() {
        assertThat(client.listDatabaseNames().first()).isNotEmpty();
        assertThat(reactiveClient.listDatabases().collect().first().await().indefinitely()).isNotEmpty();

        org.eclipse.microprofile.health.HealthCheckResponse response = health.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get()).hasSize(2).contains(entry("<default-reactive>", "OK"),
                entry("<default>", "OK"));

        // Stop the database and recheck the health
        stopMongoDatabase();

        response = health.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData()).isNotEmpty();
        assertThat(response.getData().get()).hasSize(2)
                .allSatisfy(new BiConsumer<String, Object>() {
                    @Override
                    public void accept(String s, Object o) {
                        assertThat(o.toString()).startsWith("KO, reason:");
                    }
                });
    }
}
