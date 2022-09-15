package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.json.JsonObject;

public class VertxSerdeRemovedTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(IncomingChannel.class));

    @ApplicationScoped
    public static class IncomingChannel {

        @Incoming("in")
        public void consume(JsonObject payload) {
            // no-op
        }
    }

    @Test
    void test_logged_depreciation_notice() {
        TEST.assertLogRecords(records -> {
            assertThat(records).anyMatch(log -> log.getLoggerName().contains("KafkaCodecDependencyRemovalLogger") &&
                    log.getMessage().contains("io.quarkus.kafka.client.serialization.JsonObjectDeserializer"));
        });
    }
}
