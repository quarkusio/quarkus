package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultLocaleMissingMessageTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, EnMessages.class)
                    .addAsResource(new StringAsset("goodbye=auf Wiedersehen"), "messages/msg_de.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs")
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof MessageBundleException) {
                    assertTrue(
                            rootCause.getMessage()
                                    .contains("Message template for key [goodbye] is missing for default locale"));
                } else {
                    fail("No message bundle exception thrown: " + t);
                }
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface Messages {

        @Message
        String goodbye();
    }

    @Localized("en")
    public interface EnMessages extends Messages {

        @Override
        @Message("Goodbye")
        String goodbye();

    }

}
