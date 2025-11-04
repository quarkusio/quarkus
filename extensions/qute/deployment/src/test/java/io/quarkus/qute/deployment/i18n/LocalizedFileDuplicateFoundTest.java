package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.test.QuarkusUnitTest;

public class LocalizedFileDuplicateFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("hello=Ahoj!"), "messages/messages_cs.properties"))
            // there are multiple message files of the same priority
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("hello=Cau!"), "messages/messages_cs.properties"))
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("hello=Caucau!"), "messages/messages_cs.properties"))
            .assertException(t -> {
                Throwable e = t;
                MessageBundleException mbe = null;
                while (e != null) {
                    if (e instanceof MessageBundleException) {
                        mbe = (MessageBundleException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(mbe);
                assertTrue(mbe.getMessage().contains("Duplicate localized files with priority 1 found:"), mbe.getMessage());
                assertTrue(mbe.getMessage().contains("messages_cs.properties"), mbe.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

}
