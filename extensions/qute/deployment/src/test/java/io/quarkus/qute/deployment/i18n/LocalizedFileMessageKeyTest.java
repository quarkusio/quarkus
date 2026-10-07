package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.Message.HYPHENATED_ELEMENT_NAME;
import static io.quarkus.qute.i18n.Message.UNDERSCORED_ELEMENT_NAME;
import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Localized files use the message keys instead of the method names when
 * {@code quarkus.qute.localized-file-keys=message-key} is set.
 */
public class LocalizedFileMessageKeyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, DeMessages.class)
                    .addAsResource(new StringAsset("""
                            hello_and_more=Hello {name} and more!
                            hello-and-less=Hello and less!
                            custom=Custom!
                            """), "messages/msg_en.properties")
                    .addAsResource(new StringAsset("""
                            hello_and_more=Hallo {name} und mehr!
                            hello-and-less=Hallo und weniger!
                            custom=Benutzerdefiniert!
                            hello_only_less=Ignoriert!
                            """), "messages/msg_de.properties")
                    .addAsResource(new StringAsset("""
                            hello_and_more=Ahoj {name} a více!
                            hello-and-less=Ahoj a méně!
                            custom=Vlastní!
                            """), "messages/msg_cs.properties")
                    .addAsResource(new StringAsset("{msg:hello_and_more('Malachi')} {msg:hello-and-less} {msg:custom}"),
                            "templates/foo.txt"))
            .overrideConfigKey("quarkus.default-locale", "en")
            .overrideConfigKey("quarkus.qute.localized-file-keys", "message-key");

    @Localized("en")
    Messages enMessages;

    @Localized("de")
    Messages deMessages;

    @Localized("cs")
    Messages csMessages;

    @Inject
    Template foo;

    @Test
    public void testDefaultLocaleFileIsMerged() {
        assertEquals("Hello Malachi and more!", enMessages.helloAndMore("Malachi"));
        assertEquals("Hello and less!", enMessages.helloAndLess());
        assertEquals("Custom!", enMessages.other());
        assertEquals("Hello, only less!", enMessages.helloOnlyLess());
    }

    @Test
    public void testLocalizedInterfaceIsMergedWithFile() {
        assertEquals("Hallo Malachi und mehr!", deMessages.helloAndMore("Malachi"));
        assertEquals("Hallo und weniger!", deMessages.helloAndLess());
        assertEquals("Benutzerdefiniert!", deMessages.other());
        assertEquals("Hallo, nur weniger!", deMessages.helloOnlyLess());
    }

    @Test
    public void testLocalizedFile() {
        assertEquals("Ahoj Malachi a více!", csMessages.helloAndMore("Malachi"));
        assertEquals("Ahoj a méně!", csMessages.helloAndLess());
        assertEquals("Vlastní!", csMessages.other());
        assertEquals("Hello, only less!", csMessages.helloOnlyLess());
    }

    @Test
    public void testTemplate() {
        assertEquals("Ahoj Malachi a více! Ahoj a méně! Vlastní!", foo.instance().setLocale("cs").render());
    }

    @MessageBundle(value = DEFAULT_NAME, defaultKey = UNDERSCORED_ELEMENT_NAME)
    public interface Messages {

        @Message
        String helloAndMore(String name);

        @Message(key = HYPHENATED_ELEMENT_NAME)
        String helloAndLess();

        @Message(key = "custom")
        String other();

        @Message("Hello, only less!")
        String helloOnlyLess();

    }

    @Localized("de")
    public interface DeMessages extends Messages {

        @Message("Hallo, nur weniger!")
        String helloOnlyLess();

    }

}
