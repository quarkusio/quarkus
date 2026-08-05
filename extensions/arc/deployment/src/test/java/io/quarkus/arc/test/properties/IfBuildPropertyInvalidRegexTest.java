package io.quarkus.arc.test.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusExtensionTest;

public class IfBuildPropertyInvalidRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServiceAlpha.class))
            .overrideConfigKey("build.version", "1.0.0")
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertThat(rootCause)
                        .isInstanceOf(java.util.regex.PatternSyntaxException.class);
            });

    @Test
    public void testFailure() {
        fail();
    }

    @IfBuildProperty(name = "build.version", stringValue = "[invalid(", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha {

        public String ping() {
            return "alpha";
        }
    }
}
