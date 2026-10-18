package io.quarkus.mongodb;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.tracing.MongoTracingCommandListener;
import io.quarkus.test.QuarkusExtensionTest;

public class MongoTracingConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-tracing-mongoclient.properties");

    @Inject
    MongoTracingCommandListener listener;

    @Test
    public void testTracingListenerIsRegistered() {
        assertNotNull(listener);
    }
}
