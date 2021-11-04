package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.test.QuarkusUnitTest;

public class NoExtensionMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Extensions.class))
            .assertException(t -> {
                Throwable ise = t;
                while (ise != null) {
                    if (ise instanceof IllegalStateException) {
                        break;
                    }
                    ise = ise.getCause();
                }
                assertNotNull(ise);
                assertEquals(
                        "No template extension methods declared on io.quarkus.qute.deployment.extensions.NoExtensionMethodsTest$Extensions; a template extension method must be static, non-private and must not return void",
                        ise.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @TemplateExtension
    public static class Extensions {

        // this method is ignored
        public String foo() {
            return "ok";
        }
    }

}
