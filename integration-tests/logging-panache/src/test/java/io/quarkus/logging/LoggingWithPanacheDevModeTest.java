package io.quarkus.logging;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.logging.Formatter;
import java.util.stream.Collectors;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class LoggingWithPanacheDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(LoggingEndpoint.class))
            .setLogRecordPredicate(record -> "io.quarkus.logging.LoggingEndpoint".equals(record.getLoggerName()));

    @Test
    public void testRepositoryIsReloaded() {
        Formatter formatter = new PatternFormatter("[%p] %m");

        {
            when().get("/logging").then().body(is("hello"));

            List<String> lines = TEST.getLogRecords().stream().map(formatter::format).collect(Collectors.toList());
            assertThat(lines).containsExactly("[INFO] hello");
            TEST.clearLogRecords();
        }

        TEST.modifySourceFile("LoggingEndpoint.java", s -> s.replace("hello", "hi"));

        {
            when().get("/logging").then().body(is("hi"));

            List<String> lines = TEST.getLogRecords().stream().map(formatter::format).collect(Collectors.toList());
            assertThat(lines).containsExactly("[INFO] hi");
            TEST.clearLogRecords();
        }
    }
}
