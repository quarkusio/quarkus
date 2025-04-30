package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleLogicalLineTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, MyEnum.class)
                    .addAsResource("messages/msg_cs.properties")
                    .addAsResource(new StringAsset(
                            "{msg:hello('Edgar')}::{msg:helloNextLine('Edgar')}::{msg:fruits}::{msg:myEnum(MyEnum:OFF)}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testResolvers() {
        assertEquals("Hello Edgar!::Hello \n Edgar!::apple, banana, pear, watermelon, kiwi, mango::Off",
                foo.render());
        assertEquals("Ahoj Edgar a dobrý den!::Ahoj \n Edgar!::jablko, banan, hruska, meloun, kiwi, mango::Vypnuto",
                foo.instance().setLocale("cs").render());
    }

    @MessageBundle(value = DEFAULT_NAME, locale = "en")
    public interface Messages {

        @Message("Hello {name}!")
        String hello(String name);

        @Message("Hello \n {name}!")
        String helloNextLine(String name);

        @Message("apple, banana, pear, watermelon, kiwi, mango")
        String fruits();

        @Message("{#when myEnum}"
                + "{#is ON}On"
                + "{#is OFF}Off"
                + "{#else}Undefined"
                + "{/when}")
        String myEnum(MyEnum myEnum);

    }

}
