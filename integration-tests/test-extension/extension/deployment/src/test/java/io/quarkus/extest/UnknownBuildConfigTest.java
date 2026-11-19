package io.quarkus.extest;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
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
        List<LogRecord> unrecognized = logRecords.stream()
                .filter(logRecord -> logRecord.getMessage().startsWith("Unrecognized configuration property"))
                .toList();

        Map<Object, Object> unknown = unrecognized.stream()
                .collect(toMap(logRecord -> logRecord.getParameters()[0], logRecord -> logRecord.getParameters()[1]));

        // A name from an environment variable is not reported (quarkus.build.time.unknown.env)
        assertEquals(Set.of("quarkus.build-time.unknown.prop", "quarkus.default.locale", "quarkus.unknown.prop"),
                unknown.keySet());
        assertEquals("UnknownBuildPropertyConfigSource", unknown.get("quarkus.build-time.unknown.prop"));
        // A name that is only different in the punctuation from a known name is still unknown
        assertEquals("UnknownBuildPropertyConfigSource", unknown.get("quarkus.default.locale"));
    }
}
