package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageDefaultValueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Messages.class)
                    .addAsResource(new StringAsset("""
                            alpha=Hi {foo}!
                            delta=Hey {foo}!
                            """), "messages/msg_en.properties")
                    .addAsResource(new StringAsset("""
                            alpha=Ahoj {foo}!
                            delta=Hej {foo}!
                            """), "messages/msg_cs.properties")

                    .addAsResource(new StringAsset(
                            "{msg:alpha('baz')}::{msg:bravo('baz')}::{msg:charlie('baz')}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset(
                            "{msg:delta('baz')}::{msg:echo('baz')}"),
                            "templates/bar.html"))
            .overrideConfigKey("quarkus.default-locale", "en");

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Test
    public void testMessages() {
        assertEquals("Hi baz!::Bravo baz!::Hey baz!", foo.instance().setLocale("en").render());
        assertEquals("Hej baz!::Echo cs baz!", bar.instance().setLocale("cs").render());
    }

    @MessageBundle("msg")
    public interface Messages {

        // localized file wins
        @Message(defaultValue = "Alpha {foo}!")
        String alpha(String foo);

        // defaultValue is used
        @Message(defaultValue = "Bravo {foo}!")
        String bravo(String foo);

        // value() wins
        @Message(value = "Hey {foo}!", defaultValue = "Charlie {foo}!")
        String charlie(String foo);

        // msg_cs.properties wins
        @Message(defaultValue = "Delta {foo}!")
        String delta(String foo);

        // CsMessages#echo() wins
        @Message(defaultValue = "Echo {foo}!")
        String echo(String foo);

    }

    @Localized("cs")
    public interface CsMessages extends Messages {

        // msg_cs.properties wins
        @Message(defaultValue = "Delta cs {foo}!")
        @Override
        String delta(String foo);

        @Message(defaultValue = "Echo cs {foo}!")
        @Override
        String echo(String foo);

    }

}
