package io.quarkus.restclient.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("restclient-config-test-application.properties", "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                Set<String> properties = logRecords.stream().flatMap(
                        logRecord -> Stream.of(logRecord.getParameters())).map(Object::toString).collect(Collectors.toSet());
                assertEquals(0, properties.size(), "Expected 0 warnings");
            });

    @Inject
    RestClientsConfig restClientsConfig;

    @Test
    void testClientConfigsArePresent() {
        verifyClientConfig(restClientsConfig.getClientConfig("echo-client"));
        verifyClientConfig(restClientsConfig.getClientConfig("io.quarkus.restclient.configuration.EchoClient"));
        verifyClientConfig(restClientsConfig.getClientConfig("EchoClient"));
        verifyClientConfig(restClientsConfig.getClientConfig("a.b.c.Client"));
    }

    private void verifyClientConfig(RestClientConfig config) {
        assertTrue(config.url.isPresent());
        assertEquals("http://localhost:8081", config.url.get());
    }
}
