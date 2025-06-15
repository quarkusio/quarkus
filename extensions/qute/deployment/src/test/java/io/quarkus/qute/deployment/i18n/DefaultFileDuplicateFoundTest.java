package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultFileDuplicateFoundTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("hi=Nazdar!"), "messages/msg.properties")
                    .addAsResource(new StringAsset("hi=Ahoj!"), "messages/msg_cs.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs").assertException(t -> {
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
                assertTrue(mbe.getMessage().contains("localized file already exists for locale [cs]"),
                        mbe.getMessage());
                assertTrue(mbe.getMessage().contains("msg_cs.properties"), mbe.getMessage());
                assertTrue(mbe.getMessage().contains("msg.properties"), mbe.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle(DEFAULT_NAME)
    public interface Messages {
        @Message
        String hi();
    }

}
