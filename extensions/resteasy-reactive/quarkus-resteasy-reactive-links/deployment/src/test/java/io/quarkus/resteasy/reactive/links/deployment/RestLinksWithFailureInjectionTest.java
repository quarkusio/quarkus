package io.quarkus.resteasy.reactive.links.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class RestLinksWithFailureInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(TestRecordNoId.class, TestResourceNoId.class)).assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertThat(rootCause).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Cannot generate web links for the class " +
                                "io.quarkus.resteasy.reactive.links.deployment.TestRecordNoId because is either " +
                                "missing an `id` field or a field with an `@Id` annotation");
            });

    @Test
    void validationFailed() {
        // Should not be reached: verify
        assertTrue(false);
    }
}
