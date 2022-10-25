package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.test.QuarkusUnitTest;

public class ArcLookupProblemDetectedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Alpha.class))
            .setLogRecordPredicate(log -> ArcContainerImpl.class.getPackage().getName().equals(log.getLoggerName()))
            .assertLogRecords(records -> {
                LogRecord warning = records.stream()
                        .filter(l -> l.getMessage().contains("programmatic lookup problem detected")).findAny().orElse(null);
                assertNotNull(warning);
                Formatter fmt = new PatternFormatter("%m");
                String message = fmt.format(warning);
                assertTrue(message.contains(
                        "Stack frame: io.quarkus.arc.test.unused.ArcLookupProblemDetectedTest.testWarning"),
                        message);
                assertTrue(message.contains(
                        "Required type: class io.quarkus.arc.test.unused.ArcLookupProblemDetectedTest$Alpha"),
                        message);
            });

    @Test
    public void testWarning() {
        // Note that the warning is only displayed once, subsequent calls use a cached result
        assertFalse(CDI.current().select(Alpha.class).isResolvable());
    }

    // unused bean, will be removed
    @ApplicationScoped
    static class Alpha {

        public String ping() {
            return "ok";
        }

    }

}
