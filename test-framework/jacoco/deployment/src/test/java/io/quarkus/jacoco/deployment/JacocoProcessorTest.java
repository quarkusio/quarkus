package io.quarkus.jacoco.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class JacocoProcessorTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClass(Covered.class))
            .overrideConfigKey("quarkus.jacoco.report", "false");

    @Test
    void shouldInstrumentClassesInQuarkusExtensionTest() {
        assertThat(Covered.class.getDeclaredMethods())
                .extracting(Method::getName)
                .contains("$jacocoInit");
    }

    static class Covered {

        void ping() {
        }
    }
}
