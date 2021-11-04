package io.quarkus.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingWithPanacheGeneratedTest {
    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GeneratedBean.class, NoStackTraceTestException.class))
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.logging\".min-level", "TRACE")
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.logging\".level", "TRACE");

    @Test
    public void test() {
        new GeneratedBean().testLogging();
        // no asserts, this test only verifies that all Log methods can be successfully invoked
        // (in other words, that the bytecode transformation isn't horribly broken)
    }
}
