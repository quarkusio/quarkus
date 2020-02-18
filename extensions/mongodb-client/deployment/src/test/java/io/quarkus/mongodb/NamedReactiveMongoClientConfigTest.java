package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.runtime.MongoClientName;
import io.quarkus.test.QuarkusUnitTest;

public class NamedReactiveMongoClientConfigTest extends MongoWithReplicasTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("application-named-mongoclient.properties");

    @Inject
    @MongoClientName("cluster1")
    ReactiveMongoClient legacyClient;

    @Inject
    @MongoClientName("cluster1")
    io.quarkus.mongodb.reactive.ReactiveMongoClient client;

    @Inject
    @MongoClientName("cluster2")
    ReactiveMongoClient legacyClient2;

    @Inject
    @MongoClientName("cluster2")
    io.quarkus.mongodb.reactive.ReactiveMongoClient client2;

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
        if (client2 != null) {
            client2.close();
        }
        if (legacyClient != null) {
            legacyClient.close();
        }
        if (legacyClient2 != null) {
            legacyClient2.close();
        }
    }

    @Test
    public void testNamedDataSourceInjection() {
        assertThat(client.listDatabases().collectItems().first().await().indefinitely()).isNotEmpty();
        assertThat(client2.listDatabases().collectItems().first().await().indefinitely()).isNotEmpty();
        assertThat(legacyClient.listDatabases().findFirst().run().toCompletableFuture().join()).isNotEmpty();
        assertThat(legacyClient2.listDatabases().findFirst().run().toCompletableFuture().join()).isNotEmpty();
    }
}
