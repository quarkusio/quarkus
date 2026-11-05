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
 * The blocking and the reactive client are separate driver clients created at startup, so only the injected type of
 * each client is registered.
 */
public class MongoInjectedClientTypesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.connection-string", "mongodb://localhost:9999")
            .overrideConfigKey("quarkus.mongodb.second.connection-string", "mongodb://localhost:9999");

    @Inject
    MongoClient client;

    @Inject
    @MongoClientName("second")
    ReactiveMongoClient secondClient;

    @Test
    public void onlyInjectedTypesAreRegistered() {
        assertThat(Arc.container().select(MongoClient.class).isResolvable()).isTrue();
        assertThat(Arc.container().select(ReactiveMongoClient.class).isUnsatisfied()).isTrue();

        MongoClientName second = MongoClientName.Literal.of("second");
        assertThat(Arc.container().select(ReactiveMongoClient.class, second).isResolvable()).isTrue();
        assertThat(Arc.container().select(MongoClient.class, second).isUnsatisfied()).isTrue();
    }
}
