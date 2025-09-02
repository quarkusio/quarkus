package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class UnknownConfigFilesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(EmptyAsset.INSTANCE, "application.properties")
                    .addAsResource(EmptyAsset.INSTANCE, "application-prod.properties")
                    .addAsResource(EmptyAsset.INSTANCE, "application.yaml")
                    .addAsResource(EmptyAsset.INSTANCE, "application-test.toml"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                List<LogRecord> unknownConfigFiles = logRecords.stream()
                        .filter(l -> l.getMessage().startsWith("Unrecognized configuration file"))
                        .toList();
                assertEquals(1, unknownConfigFiles.size());
                assertTrue(unknownConfigFiles.get(0).getParameters()[0].toString().contains("application.yaml"));

                List<LogRecord> profiledConfigFiles = logRecords.stream()
                        .filter(l -> l.getMessage().startsWith("Profiled configuration file"))
                        .toList();
                assertEquals(1, profiledConfigFiles.size());
                assertTrue(profiledConfigFiles.get(0).getParameters()[0].toString().contains("application-test.toml"));
            });

    @Test
    void unknownConfigFiles() {

    }
}
