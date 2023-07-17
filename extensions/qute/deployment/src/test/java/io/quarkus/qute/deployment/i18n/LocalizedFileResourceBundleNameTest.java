package io.quarkus.qute.deployment.i18n;

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
import io.quarkus.test.QuarkusUnitTest;

public class LocalizedFileResourceBundleNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(Messages1.class)
                    .addAsResource(new StringAsset("{msg:hello}"), "templates/foo.html")
                    .addAsResource(new StringAsset("hello=Hello!"), "messages/msg_en_US.properties")
                    .addAsResource(new StringAsset("hello=Ahoj!"), "messages/msg_cs_CZ.properties"))
            .overrideConfigKey("quarkus.default-locale", "en-US");

    @Inject
    Messages1 messages;

    @Localized("cs-CZ")
    Messages1 csMessages;

    @Inject
    Template foo;

    @Test
    public void testLocalizedFile() {
        assertEquals("Hello!", messages.hello());
        assertEquals("Ahoj!", csMessages.hello());

        assertEquals("Hello!", foo.instance().render());
        assertEquals("Ahoj!", foo.instance().setAttribute("locale", "cs-CZ").render());
    }

    @MessageBundle(DEFAULT_NAME)
    public interface Messages1 {

        @Message
        String hello();

    }

}
