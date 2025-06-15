package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class LocalizedBundleLocaleConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Messages.class, EnMessages.class, AnotherEnMessages.class))
            .overrideConfigKey("quarkus.default-locale", "cs").assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof MessageBundleException) {
                    assertTrue(rootCause.getMessage()
                            .contains("a localized message bundle interface exists for locale [en]"));
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

        @Message("Ahoj svete!")
        String helloWorld();

    }

    @Localized("en")
    public interface EnMessages extends Messages {

        @Message("Hello world!")
        String helloWorld();

    }

    @Localized("en")
    public interface AnotherEnMessages extends Messages {

        @Message("Hello world!")
        String helloWorld();

    }

}
