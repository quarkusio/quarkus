package io.quarkus.mongodb;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.tracing.MongoTracingCommandListener;
import io.quarkus.test.QuarkusUnitTest;

public class MongoTracingConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-tracing-mongoclient.properties");

    @Inject
    MongoTracingCommandListener listener;

    @Test
    public void testTracingListenerIsRegistered() {
        assertNotNull(listener);
    }
}
