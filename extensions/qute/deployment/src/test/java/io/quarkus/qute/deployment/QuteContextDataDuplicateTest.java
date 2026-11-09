package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.runtime.QuteContextData;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A second copy of the template data resource, for example in a dependency, must not replace the data of the
 * application.
 */
public class QuteContextDataDuplicateTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hello!"), "templates/hello.txt")
                    .addAsResource(new StringAsset("qute-context-data 1\n"), QuteContextData.RESOURCE))
            .assertException(t -> {
                Throwable cause = t;
                while (cause != null) {
                    if (cause.getMessage() != null && cause.getMessage().contains("Only one copy of")) {
                        return;
                    }
                    cause = cause.getCause();
                }
                fail("Unexpected exception", t);
            });

    @Test
    public void testFailure() {
        fail();
    }

}
