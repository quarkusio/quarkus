package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidExtensionMethodNamespaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Extensions.class))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    public static class Extensions {

        @TemplateExtension(namespace = ":bar-")
        static String foo() {
            return "ok";
        }
    }

}
