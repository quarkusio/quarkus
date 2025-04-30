package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusUnitTest;

public class MongoTracingNotEnabledTest extends MongoTestBase {

    @Inject
    MongoClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class,
                            MockReactiveContextProvider.class))
            .withConfigurationResource("default-mongoclient.properties");

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void contextProviderMustNotBeCalledIfNoOpenTelemetryIsAvailable() {
        assertThat(client.listDatabaseNames().first()).isNotEmpty();
        assertThat(MockReactiveContextProvider.EVENTS).isEmpty();
    }

}
