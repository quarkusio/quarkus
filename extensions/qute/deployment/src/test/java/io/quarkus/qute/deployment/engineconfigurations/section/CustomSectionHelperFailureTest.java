package io.quarkus.qute.deployment.engineconfigurations.section;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class CustomSectionHelperFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(CustomSectionFactory.class, StringProducer.class)
                    .addAsResource(new StringAsset("{#custom bar=1 /}"), "templates/bar.html"))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof IllegalStateException) {
                    assertTrue(rootCause.getMessage().contains(
                            "Foo param not found"));
                } else {
                    fail(t);
                }
            });

    @Test
    public void testValidation() {
        fail();
    }

}
