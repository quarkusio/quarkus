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

public class LoggingWithPanacheKotlinTest {
    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(LoggingEntity.class, LoggingRepository.class))
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkus.logging.Logging"))
            .assertLogRecords(records -> {
                Formatter formatter = new PatternFormatter("[%p] %m");
                List<String> lines = records.stream().map(formatter::format).map(String::trim).collect(Collectors.toList());

                assertThat(lines).containsExactly(
                        "[INFO] Hello from repository",
                        "[INFO] Hello from entity");
            });

    @Inject
    LoggingRepository repository;

    @Test
    public void test() {
        repository.test();
        new LoggingEntity().test();
    }
}
