package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultFileBundleLocaleMergeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class, EnMessages.class, DeMessages.class)
                    .addAsResource(new StringAsset("hello_world=Hi!"), "messages/msg_en.properties")
                    .addAsResource(new StringAsset("farewell=Abschied\ngoodbye=Freilos"), "messages/msg_de.properties")
                    .addAsResource(new StringAsset("goodbye=Mej se!\nfarewell=Sbohem!\nhello_world=Zdravim svete!"),
                            "messages/msg.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs");

    @Localized("en")
    Messages enMessages;

    @Localized("de")
    Messages deMessages;

    @Inject
    Messages csMessages;

    /**
     * Default message template method is not overridden and message was set with {@link Message#value()}.
     */
    @Test
    void testDefaultFromAnnotationIsUsedAsFallback() {
        assertEquals("Nazdar!", enMessages.hello());
    }

    /**
     * Default message template method is not overridden and message was set in default 'msg.properties' file.
     */
    @Test
    void testDefaultFromFileIsUsedAsFallback() {
        assertEquals("Mej se!", enMessages.goodbye());
    }

    /**
     * Localized message template method is provided without {@link Message#value()}
     */
    @Test
    void testDefaultIsUsedAsFallback2() {
        assertEquals("Greetings!", enMessages.greetings());
    }

    @Test
    void testLocalizedFileIsMerged() {
        assertEquals("Freilos", deMessages.goodbye());
    }

    /**
     * Default message set with {@link Message#value()} has priority over message from 'msg.properties'.
     */
    @Test
    void testDefaultInterfaceHasPriority() {
        assertEquals("Ahoj svete!", csMessages.hello_world());
    }

    @Test
    void testBothDefaultAndLocalizedFromFile() {
        assertEquals("Abschied", deMessages.farewell());
    }

    @MessageBundle(DEFAULT_NAME)
    public interface Messages {

        @Message("Ahoj svete!")
        String hello_world();

        @Message("Nazdar!")
        String hello();

        String goodbye();

        @Message("Greetings!")
        String greetings();

        @Message
        String farewell();
    }

    @Localized("en")
    public interface EnMessages extends Messages {

        @Override
        @Message
        String greetings();

    }

    @Localized("de")
    public interface DeMessages extends Messages {

    }

}
