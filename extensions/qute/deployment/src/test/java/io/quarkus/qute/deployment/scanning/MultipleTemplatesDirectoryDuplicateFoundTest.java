package io.quarkus.qute.deployment.scanning;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipleTemplatesDirectoryDuplicateFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("Hello!"), "templates/hello.html"))
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("Hi!"), "templates/hello.html"))
            .assertException(t -> {
                Throwable e = t;
                IllegalStateException ise = null;
                while (e != null) {
                    if (e instanceof IllegalStateException) {
                        ise = (IllegalStateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(ise);
                assertTrue(ise.getMessage().contains("Duplicate templates found:"), ise.getMessage());
                assertTrue(ise.getMessage().contains("hello.html"), ise.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

}
