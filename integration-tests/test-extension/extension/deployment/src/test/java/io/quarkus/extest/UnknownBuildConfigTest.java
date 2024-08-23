package io.quarkus.extest;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class UnknownBuildConfigTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .setExpectExit(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void unknownBuildConfig() {
        List<LogRecord> logRecords = prodModeTestResults.getRetainedBuildLogRecords();

        // These are the expected unknown properties in the test extension. This could probably be improved, because
        // these are generated with the rename test. If there is a change we know that something happened.
        Set<Object> unrecognized = logRecords.stream()
                .filter(logRecord -> logRecord.getMessage().startsWith("Unrecognized configuration key"))
                .map(logRecord -> Optional.ofNullable(logRecord.getParameters())
                        .map(parameters -> parameters[0])
                        .orElse(new Object[0]))
                .collect(toSet());

        assertEquals(2, unrecognized.size());
        assertTrue(unrecognized.contains("quarkus.unknown.prop"));
        assertTrue(unrecognized.contains("quarkus.build.unknown.prop"));
    }
}
