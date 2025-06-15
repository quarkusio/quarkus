package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class LocalizedFileDefaultLocaleMergeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(Messages.class)
                    .addAsResource(new StringAsset("hello=Hi!\ngoodbye=Bye"), "messages/msg_en.properties"))
            .overrideConfigKey("quarkus.default-locale", "en");

    @Localized("en")
    Messages messages;

    @Test
    public void testInterfaceHasPriority() {
        assertEquals("Hello", messages.hello());
    }

    @Test
    public void testLocalizedFileIsMerged() {
        assertEquals("Bye", messages.goodbye());
    }

    @Test
    public void testInterfaceIsMerged() {
        assertEquals("Hello world!", messages.helloWorld());
    }

    @MessageBundle(DEFAULT_NAME)
    public interface Messages {

        @Message("Hello world!")
        String helloWorld();

        @Message("Hello")
        String hello();

        @Message
        String goodbye();

    }

}
