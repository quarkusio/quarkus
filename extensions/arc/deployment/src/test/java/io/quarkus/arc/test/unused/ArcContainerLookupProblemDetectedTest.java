package io.quarkus.arc.test.unused;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.test.QuarkusUnitTest;

public class ArcContainerLookupProblemDetectedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Alpha.class))
            .setLogRecordPredicate(log -> ArcContainerImpl.class.getPackage().getName().equals(log.getLoggerName()))
            .assertLogRecords(records -> {
                LogRecord warning = records.stream()
                        .filter(l -> l.getMessage().contains("programmatic lookup problem detected")).findAny().orElse(null);
                assertThat(warning).isNotNull();
                assertThat(warning.getParameters()).hasSizeGreaterThanOrEqualTo(5);
                assertThat(warning.getParameters()[2])
                        .isInstanceOf(Class.class)
                        // the `Class` objects are different due to Quarkus class loading, so we have to compare names
                        .extracting("name")
                        .isEqualTo(Alpha.class.getName());
                assertThat(warning.getParameters()[4])
                        .isInstanceOf(StackWalker.StackFrame.class)
                        .extracting("className", "methodName")
                        .containsExactly(ArcContainerLookupProblemDetectedTest.class.getName(), "testWarning");
            });

    @Test
    public void testWarning() {
        // Note that the warning is only displayed once, subsequent calls use a cached result
        assertFalse(Arc.container().instance(Alpha.class).isAvailable());
    }

    // unused bean, will be removed
    @ApplicationScoped
    static class Alpha {

        public String ping() {
            return "ok";
        }

    }

}
