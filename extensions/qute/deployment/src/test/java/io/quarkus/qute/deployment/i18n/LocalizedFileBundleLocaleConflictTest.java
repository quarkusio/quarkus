package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class LocalizedFileBundleLocaleConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, EnMessages.class)
                    // This localized file conflicts with the default locale
                    .addAsResource(new StringAsset(
                            "hello=Hello!"),
                            "messages/msg_en.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs")
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof MessageBundleException) {
                    assertEquals(
                            "Cannot register [msg_en.properties] - a localized message bundle interface exists for locale [en]: io.quarkus.qute.deployment.i18n.LocalizedFileBundleLocaleConflictTest$EnMessages",
                            rootCause.getMessage());
                } else {
                    fail("No message bundle exception thrown: " + t);
                }

            });;

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface Messages {

        @Message("Ahoj svete!")
        String helloWorld();

    }

    @Localized("en")
    public interface EnMessages extends Messages {

        @Message("Hello world!")
        String helloWorld();

    }

}
