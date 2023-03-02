package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleDefaultedNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Controller.class)
                    .addAsResource(new StringAsset(
                            "{Controller_index:hello(name)}"),
                            "templates/Controller/index.html")
                    .addAsResource(new StringAsset("hello=Ahoj {name}!"), "messages/Controller_index_cs.properties"));

    @Test
    public void testBundle() {
        assertEquals("Hello world!",
                Controller.Templates.index("world").render());
        assertEquals("Ahoj svete!", Controller.Templates.index("svete")
                .setAttribute(MessageBundles.ATTRIBUTE_LOCALE, Locale.forLanguageTag("cs")).render());
    }

}
