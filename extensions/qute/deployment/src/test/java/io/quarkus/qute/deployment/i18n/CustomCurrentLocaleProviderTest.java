package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.CurrentLocaleProvider;
import io.quarkus.qute.i18n.LocaleAware;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A custom {@link CurrentLocaleProvider} overrides the default one and drives the resolution of a bean injected with
 * {@link LocaleAware}.
 */
public class CustomCurrentLocaleProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(Messages.class, CzechMessages.class, CustomCurrentLocaleProvider.class))
            .overrideConfigKey("quarkus.default-locale", "en");

    @LocaleAware
    Messages messages;

    @Test
    public void testCustomProviderIsUsed() {
        assertEquals("Ahoj!", messages.hello());
        assertEquals("Ahoj Bobe!", messages.hello_name("Bobe"));
    }

    @Singleton
    public static class CustomCurrentLocaleProvider implements CurrentLocaleProvider {

        @Override
        public Locale currentLocale() {
            return Locale.of("cs");
        }

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
