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

public class DefaultFileDefaultBundleNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Messages.class)
                    .addAsResource(new StringAsset("goodbye=Mej se!\nfarewell=Sbohem!"), "messages/msg.properties"))
            .overrideConfigKey("quarkus.default-locale", "cs");

    @Inject
    Messages csMessages1;

    @Localized("cs")
    Messages csMessages2;

    @Test
    void unannotatedMessageMethod() {
        assertEquals("Mej se!", csMessages1.goodbye());
    }

    @Test
    void annotatedMessageMethod() {
        assertEquals("Sbohem!", csMessages2.farewell());
    }

    @MessageBundle(DEFAULT_NAME)
    public interface Messages {

        String goodbye();

        @Message
        String farewell();
    }

}
