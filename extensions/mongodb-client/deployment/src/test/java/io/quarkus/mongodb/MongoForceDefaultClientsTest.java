package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With {@code quarkus.mongodb.force-default-clients}, both default clients are registered, even when only one of them
 * is injected.
 */
public class MongoForceDefaultClientsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.connection-string", "mongodb://localhost:9999")
            .overrideConfigKey("quarkus.mongodb.force-default-clients", "true");

    @Inject
    MongoClient client;

    @Test
    public void bothDefaultClientsAreRegistered() {
        assertThat(Arc.container().select(MongoClient.class).isResolvable()).isTrue();
        assertThat(Arc.container().select(ReactiveMongoClient.class).isResolvable()).isTrue();
    }
}
