package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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
        List<LogRecord> unrecognized = logRecords.stream()
                .filter(logRecord -> logRecord.getMessage().startsWith("Unrecognized configuration property"))
                .toList();

        assertEquals(2, unrecognized.size());
        assertEquals("quarkus.build-time.unknown.prop", unrecognized.get(0).getParameters()[0]);
        assertEquals("UnknownBuildPropertyConfigSource", unrecognized.get(0).getParameters()[1]);
        assertEquals("quarkus.unknown.prop", unrecognized.get(1).getParameters()[0]);
    }
}
