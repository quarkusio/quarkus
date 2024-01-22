package io.quarkus.resteasy.reactive.links.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class RestLinksWithFailureInjectionMultipleRestLinkIdTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(TestRecordMultipleRestLinkIds.class, TestResourceMultipleRestLinkIds.class))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertThat(rootCause).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Cannot generate web links for the class " +
                                "io.quarkus.resteasy.reactive.links.deployment.TestRecordMultipleRestLinkIds" +
                                " because it has multiple fields annotated with `@RestLinkId`, where a maximum of one is allowed");
            });

    @Test
    void validationFailed() {
        // Should not be reached: verify
        fail();
    }
}
