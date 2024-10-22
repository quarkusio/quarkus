package io.quarkus.restclient.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(EchoClient.class)
                    .addAsResource("restclient-config-test-application.properties", "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> assertFalse(logRecords.stream()
                    .map(LogRecord::getMessage)
                    .anyMatch(message -> message.startsWith("Unrecognized configuration key"))));

    @Inject
    RestClientsConfig restClientsConfig;

    @Test
    void testClientConfigsArePresent() {
        verifyClientConfig(restClientsConfig.clients().get("io.quarkus.restclient.configuration.EchoClient"));
        assertFalse(restClientsConfig.clients().containsKey("echo-client"));
        assertFalse(restClientsConfig.clients().containsKey("EchoClient"));
        assertFalse(restClientsConfig.clients().containsKey("a.b.c.Client"));
    }

    private void verifyClientConfig(RestClientsConfig.RestClientConfig config) {
        assertTrue(config.url().isPresent());
        assertEquals("http://localhost:8081", config.url().get());
    }
}
