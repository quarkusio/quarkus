package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

public class MongoTracingEnabled extends MongoTestBase {

    @Inject
    ReactiveMongoClient reactiveClient;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MongoTestBase.class, MockReactiveContextProvider.class, MockCommandListener.class))
            .withConfigurationResource("application-tracing-mongoclient.properties");

    @AfterEach
    void cleanup() {
        if (reactiveClient != null) {
            reactiveClient.close();
        }
    }

    @Test
    void invokeReactiveContextProvider() {
        String dbNames = reactiveClient.listDatabaseNames().toUni().await().atMost(Duration.ofSeconds(30L));
        assertThat(dbNames).as("expect db names available").isNotBlank();
        await().atMost(Duration.ofSeconds(30L)).untilAsserted(
                () -> assertThat(MockReactiveContextProvider.EVENTS)
                        .as("reactive context provider must be called")
                        .isNotEmpty());
        assertThat(MockCommandListener.EVENTS).isNotEmpty();

    }

}
