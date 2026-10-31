package io.quarkus.extest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class UnknownRuntimeConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset("""
                    quarkus.unknown-runtime.prop=value
                    quarkus.unknown-runtime.list[0]=value
                    quarkus.application.name=known-build-and-run-time-fixed
                    quarkus.package.output-name=known-build-time
                    quarkus.log.level=INFO
                    not-quarkus.prop=value
                    """), "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> assertThat(logRecords.stream()
                    .filter(logRecord -> logRecord.getMessage().startsWith("Unrecognized configuration property"))
                    .map(LogRecord::getParameters)
                    .map(parameters -> parameters[0]))
                    .containsExactly("quarkus.unknown-runtime.prop"));

    @Test
    void reportsOnlyUnknownNamesOfAllPhases() {
    }
}
