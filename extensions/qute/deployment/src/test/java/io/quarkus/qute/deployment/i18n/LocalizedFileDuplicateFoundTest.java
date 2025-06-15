package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LocalizedFileDuplicateFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addAsResource(new StringAsset("hello=Ahoj!"), "messages/messages_cs.properties"))
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("hello=Cau!"), "messages/messages_cs.properties"))
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
                assertTrue(ise.getMessage().contains("Duplicate localized files found:"), ise.getMessage());
                assertTrue(ise.getMessage().contains("messages_cs.properties"), ise.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

}
