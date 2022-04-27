package io.quarkus.extest;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
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

        Optional<LogRecord> unknownBuildKey = logRecords.stream()
                .filter(logRecord -> asList(Optional.ofNullable(logRecord.getParameters()).orElse(new Object[0]))
                        .contains("quarkus.build.unknown.prop"))
                .findFirst();
        assertTrue(unknownBuildKey.isPresent());
        assertTrue(unknownBuildKey.get().getMessage().startsWith("Unrecognized configuration key"));
    }
}
