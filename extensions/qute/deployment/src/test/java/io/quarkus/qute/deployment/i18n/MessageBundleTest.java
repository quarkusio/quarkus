package io.quarkus.qute.deployment.i18n;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AppMessages.class, OtherMessages.class, AlphaMessages.class, Item.class)
                    .addAsResource(new StringAsset(
                            "{msg:hello} {msg:hello_name('Jachym')} {msg:hello_with_if_section(3)} {alpha:hello-alpha} {alpha:hello_alpha} {alpha:hello-with-param('foo')}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset(
                            "hello=Hallo Welt!\nhello_name=Hallo {name}!"),
                            "messages/msg_de.properties")
                    .addAsResource(new StringAsset(
                            "{msg:message('hello')} {msg:message(key,'Malachi',surname)}"),
                            "templates/dynamic.html"));

    @Inject
    AppMessages messages;

    @Localized("cs")
    AppMessages czechMessages;

    // This one is backed by a file
    @Localized("de")
    AppMessages germanMessages;

    @Inject
    Template foo;

    @Inject
    Engine engine;

    @Test
    public void testMessageBundles() {
        assertEquals("Hello Jachym!", MessageBundles.get(AppMessages.class).hello_name("Jachym"));
        assertEquals("Hello you guy!", MessageBundles.get(AppMessages.class, Localized.Literal.of("cs")).helloWithIfSection(1));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> MessageBundles.get(String.class))
                .withMessage(
                        "Not a message bundle interface: java.lang.String");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> MessageBundles.get(Engine.class))
                .withMessage(
                        "Message bundle interface must be annotated either with @MessageBundle or with @Localized: io.quarkus.qute.Engine");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> MessageBundles.get(AppMessages.class, Localized.Literal.of("hu")))
                .withMessage(
                        "Unable to obtain a message bundle for interface [io.quarkus.qute.deployment.i18n.AppMessages] and locale [hu]");
    }

    @Test
    public void testBeans() {
        assertEquals("Hello Jachym!", messages.hello_name("Jachym"));
        assertEquals("Item name: axe, age: 2", messages.itemDetail(new Item("axe", 2)));
        assertEquals("Ahoj Jachym!", czechMessages.hello_name("Jachym"));
        assertEquals("Hello you guy!", czechMessages.helloWithIfSection(1));
        assertEquals("Hallo Welt!", germanMessages.hello());
    }

    @Test
    public void testResolvers() {
        assertEquals("Hello world! Hello Jachym! Hello you guys! Hello alpha! Hello! Hello foo from alpha!",
                foo.instance().render());
        assertEquals("Hello world! Ahoj Jachym! Hello you guys! Hello alpha! Hello! Hello foo from alpha!",
                foo.instance().setAttribute(MessageBundles.ATTRIBUTE_LOCALE, Locale.forLanguageTag("cs")).render());
        assertEquals("Hallo Welt! Hallo Jachym! Hello you guys! Hello alpha! Hello! Hello foo from alpha!",
                foo.instance().setAttribute(MessageBundles.ATTRIBUTE_LOCALE, Locale.GERMAN).render());
        assertEquals("Dot test!", engine.parse("{msg:['dot.test']}").render());
        assertEquals("Hello world! Hello Malachi Constant!",
                engine.getTemplate("dynamic").data("key", "hello_fullname").data("surname", "Constant").render());

        assertEquals("There are no files on C.",
                engine.parse("{msg:files(0,'C')}").render());
        assertEquals("There is one file on D.",
                engine.parse("{msg:files(1,'D')}").render());
        assertEquals("There are 100 files on E.",
                engine.parse("{msg:files(100,'E')}").render());

        // Test the convenient Qute class
        assertEquals("There are no files on C.", Qute.fmt("{msg:files(0,'C')}").render());
        assertEquals("Hallo Welt!", Qute.fmt("{msg:hello}").attribute("locale", Locale.GERMAN).render());
    }

}
