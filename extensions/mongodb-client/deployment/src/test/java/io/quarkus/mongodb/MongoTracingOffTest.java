package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.mongodb.runtime.MongoTracingRuntimeConfig.CommandDetailLevel;
import io.quarkus.mongodb.tracing.MongoTracingCommandListener;
import io.quarkus.test.QuarkusUnitTest;

public class MongoTracingOffTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application-tracing-off.properties");

    @Inject
    MongoTracingCommandListener listener;

    @Inject
    MongoConfig mongoConfig;

    @Test
    void testTracingListenerIsRegistered() {
        assertNotNull(listener);
    }

    @Test
    void testOffConfiguration() {
        assertThat(mongoConfig.tracing().commandDetailLevel())
                .isEqualTo(CommandDetailLevel.OFF);
    }
}
