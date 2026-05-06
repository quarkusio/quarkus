package io.quarkus.arc.test.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.properties.StringValueMatch;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusExtensionTest;

public class LookupIfPropertyInvalidRegexTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServiceAlpha.class))
            .overrideConfigKey("service.version", "1.0.0")
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                assertThat(rootCause)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("[invalid(");
            });

    @Test
    public void testFailure() {
        fail();
    }

    @LookupIfProperty(name = "service.version", stringValue = "[invalid(", match = StringValueMatch.REGEX)
    @Singleton
    static class ServiceAlpha {

        public String ping() {
            return "alpha";
        }
    }
}
