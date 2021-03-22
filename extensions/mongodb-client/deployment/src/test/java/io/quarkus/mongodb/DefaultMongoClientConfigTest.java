package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

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
    }
}
