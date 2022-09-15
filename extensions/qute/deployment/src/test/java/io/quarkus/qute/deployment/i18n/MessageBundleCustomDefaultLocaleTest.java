package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleCustomDefaultLocaleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, EnMessages.class)
                    .addAsResource(new StringAsset(
                            "{msg:helloWorld}"),
                            "templates/foo.html"))
            .overrideConfigKey("quarkus.default-locale", "cs_CZ");

    @Inject
    Template foo;

    @Test
    public void testResolvers() {
        assertEquals("Ahoj světe!", foo.render());
        assertEquals("Ahoj světe!", foo.instance().setAttribute("locale", Locale.forLanguageTag("cs")).render());
        assertEquals("Hello world!", foo.instance().setAttribute("locale", Locale.ENGLISH).render());
    }

    @MessageBundle
    public interface Messages {

        @Message("Ahoj světe!")
        String helloWorld();

    }

    @Localized("en")
    public interface EnMessages extends Messages {

        @Message("Hello world!")
        String helloWorld();

    }

}
