package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.LocaleAware;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When there is no {@code CurrentLocaleProvider} available (e.g. a non-web application) a bean injected with
 * {@link LocaleAware} falls back to the default locale.
 */
public class LocaleAwareFallbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, CzechMessages.class))
            .overrideConfigKey("quarkus.default-locale", "en");

    @LocaleAware
    Messages messages;

    @Test
    public void testFallbackToDefaultLocale() {
        assertEquals("Hello!", messages.hello());
        assertEquals("Hello Honza!", messages.hello_name("Honza"));
    }

    @MessageBundle
    public interface Messages {

        @Message("Hello!")
        String hello();

        @Message("Hello {name}!")
        String hello_name(String name);

    }

    @Localized("cs")
    public interface CzechMessages extends Messages {

        @Override
        @Message("Ahoj!")
        String hello();

        @Override
        @Message("Ahoj {name}!")
        String hello_name(String name);

    }

}
