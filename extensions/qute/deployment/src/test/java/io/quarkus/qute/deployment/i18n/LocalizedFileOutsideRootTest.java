package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.Localized;
import io.quarkus.test.QuarkusExtensionTest;

public class LocalizedFileOutsideRootTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClass(BravoMessages.class))
            .overrideConfigKey("quarkus.default-locale", "en")
            .withAdditionalDependency(
                    d -> d.addAsResource(new StringAsset("hello=Ahoj!"), "messages/msg_cs.properties"));

    @Localized("cs")
    BravoMessages messages;

    @Test
    public void testLocalizedFile() {
        assertEquals("Ahoj!", messages.hello());
    }

}
