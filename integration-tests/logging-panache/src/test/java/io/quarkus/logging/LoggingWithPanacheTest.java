package io.quarkus.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingWithPanacheTest {
    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(LoggingBean.class, LoggingInterface.class, LoggingEntity.class,
                    NoStackTraceTestException.class))
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.logging\".min-level", "TRACE")
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.logging\".level", "TRACE")
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkus.logging.Logging"))
            .assertLogRecords(records -> {
                Formatter formatter = new PatternFormatter("[%p] %m");
                List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                assertThat(lines).containsExactly(
                        "[INFO] Heya!",
                        "[TRACE] LoggingBean created",
                        "[INFO] Default method from interface: abc",
                        "[DEBUG] starting massive computation",
                        "[DEBUG] one: 42",
                        "[TRACE] two: 42 | 13",
                        "[DEBUG] three: 42 | 13 | 1",
                        "[DEBUG] one: foo",
                        "[INFO] two: foo | bar",
                        "[WARN] three: foo | bar | baz",
                        "[ERROR] four: foo | bar | baz | quux",
                        "[WARN] foo | bar | baz | quux: io.quarkus.logging.NoStackTraceTestException",
                        "[ERROR] Hello Error: io.quarkus.logging.NoStackTraceTestException",
                        "[INFO] Hi!",
                        "[INFO] number 42",
                        "[INFO] string now");
            });

    @Inject
    LoggingBean bean;

    @Test
    public void test() {
        bean.doSomething();
        new LoggingEntity().something();

        bean.reproduceStackDisciplineIssue();
    }
}
