package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.Message.UNDERSCORED_ELEMENT_NAME;
import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A localized file keyed by method names fails the build when
 * {@code quarkus.qute.localized-file-keys=message-key} is set.
 */
public class LocalizedFileMessageKeyNotFoundTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class)
                    .addAsResource(new StringAsset("helloAndMore=Hallo und mehr!"), "messages/msg_de.properties"))
            .overrideConfigKey("quarkus.qute.localized-file-keys", "message-key")
            .assertException(t -> {
                assertTrue(t instanceof MessageBundleException, t.toString());
                assertTrue(t.getMessage().contains("helloAndMore"), t.getMessage());
                assertTrue(t.getMessage().contains("hello_and_more"), t.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle(value = DEFAULT_NAME, defaultKey = UNDERSCORED_ELEMENT_NAME)
    public interface Messages {

        @Message("Hello and more!")
        String helloAndMore();

    }

}
