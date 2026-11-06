package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.i18n.acme_trans.TransMessages;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the locale suffix of a localized file is only stripped from the end of the generated class name, i.e. the
 * generated class is correctly recognized as an application class even if the package name contains the same sequence
 * (the {@code _tr} in the {@code acme_trans} package name).
 */
public class LocalePackageNameCollisionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(TransMessages.class)
                    .addAsResource(new StringAsset("hello=Hello!"), "messages/msg.properties")
                    .addAsResource(new StringAsset("hello=Merhaba!"), "messages/msg_tr.properties"));

    @Localized("tr")
    TransMessages trMessages;

    @Test
    public void testLocalizedFileInPackageWithLocaleSequence() {
        assertEquals("Merhaba!", trMessages.hello());
    }

}
