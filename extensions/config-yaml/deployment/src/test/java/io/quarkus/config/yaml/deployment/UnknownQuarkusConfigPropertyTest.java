package io.quarkus.config.yaml.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class UnknownQuarkusConfigPropertyTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.log.level: INFO\n" +
                            "quarkus:\n" +
                            "  log:\n" +
                            "    level: INFO\n"),
                    "application.yaml"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> assertTrue(logRecords.stream()
                    .map(LogRecord::getMessage)
                    .anyMatch(message -> message.startsWith("Unrecognized configuration key"))));;

    @Test
    void unknownConfig() {

    }
}
