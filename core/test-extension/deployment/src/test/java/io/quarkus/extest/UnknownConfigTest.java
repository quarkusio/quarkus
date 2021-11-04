package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

public class UnknownConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                Set<String> properties = logRecords.stream().flatMap(
                        logRecord -> Stream.of(logRecord.getParameters())).map(Object::toString).collect(Collectors.toSet());
                assertEquals(1, properties.size());
                assertTrue(properties.contains("quarkus.unknown.prop"));
            });

    @Inject
    Config config;
    @Inject
    HttpBuildTimeConfig httpBuildTimeConfig;
    @Inject
    HttpConfiguration httpConfiguration;

    @Test
    void unknown() {
        assertEquals("1234", config.getConfigValue("quarkus.unknown.prop").getValue());
        assertEquals("/1234", httpBuildTimeConfig.nonApplicationRootPath);
        assertEquals(4443, httpConfiguration.sslPort);
    }
}
