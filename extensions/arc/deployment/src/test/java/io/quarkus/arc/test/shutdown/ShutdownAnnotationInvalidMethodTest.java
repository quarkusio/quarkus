package io.quarkus.arc.test.shutdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Shutdown;
import io.quarkus.test.QuarkusUnitTest;

public class ShutdownAnnotationInvalidMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(ShutdownMethods.class))
            .setLogRecordPredicate(r -> r.getLoggerName().contains("ShutdownBuildSteps")).assertLogRecords(list -> {
                assertEquals(1, list.size());
                assertTrue(list.get(0).getMessage().startsWith("Ignored an invalid @Shutdown method declared on"));
            });

    @Test
    public void test() {
    }

    // @ApplicationScoped is added automatically
    static class ShutdownMethods {

        @Shutdown
        void invalid(String name) {
        }

    }

}
