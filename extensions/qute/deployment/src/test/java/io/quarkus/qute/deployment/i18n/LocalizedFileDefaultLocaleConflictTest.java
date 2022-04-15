package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class LocalizedFileDefaultLocaleConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class)
                    // This localized file conflicts with the default locale
                    .addAsResource(new StringAsset(
                            "hello=Hello!"),
                            "messages/msg_en.properties"))
            .overrideConfigKey("quarkus.default-locale", "en")
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof MessageBundleException) {
                    assertEquals(
                            "Locale of [msg_en.properties] conflicts with the locale [en] of the default message bundle [io.quarkus.qute.deployment.i18n.LocalizedFileDefaultLocaleConflictTest$Messages]",
                            rootCause.getMessage());
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

        @Message("Hello world!")
        String helloWorld();

    }

}
