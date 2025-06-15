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

public class LocalizedFileBundleLocaleMergeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Messages.class, EnMessages.class, DeMessages.class)
                    .addAsResource(new StringAsset("hello_world=Hi!\ngoodbye=Bye"), "messages/msg_en.properties")
                    .addAsResource(new StringAsset("farewell=Abschied"), "messages/msg_de.properties")
                    .addAsResource(new StringAsset("goodbye=Mej se!\nfarewell=Sbohem!"), "messages/msg_cs.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs");

    @Localized("en")
    Messages enMessages;

    @Localized("de")
    Messages deMessages;

    /**
     * Default message template method is not overridden.
     */
    @Test
    public void testDefaultIsUsedAsFallback() {
        assertEquals("Nazdar!", enMessages.hello());
    }

    /**
     * Localized message template method is provided without {@link Message#value()}
     */
    @Test
    public void testDefaultIsUsedAsFallback2() {
        assertEquals("Greetings!", enMessages.greetings());
    }

    @Test
    public void testLocalizedFileIsMerged() {
        assertEquals("Bye", enMessages.goodbye());
    }

    @Test
    public void testLocalizedInterfaceHasPriority() {
        assertEquals("Hello world!", enMessages.hello_world());
    }

    @Test
    public void testBothDefaultAndLocalizedFromFile() {
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

        @Message("Hello world!")
        String hello_world();

        @Message
        String greetings();

    }

    @Localized("de")
    public interface DeMessages extends Messages {

    }

}
