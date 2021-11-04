package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidExtensionMethodMatchRegexTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, Extensions.class))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    public static class Extensions {

        // the method should have at least two params and the second one must be a string
        @TemplateExtension(matchRegex = "bar")
        static String fooRegex(Foo foo) {
            return foo.name.toUpperCase();
        }
    }

}
