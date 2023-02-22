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
                    .addClasses(Views.class)
                    .addAsResource(new StringAsset(
                            "{msg_Views_Index:hello(name)}"),
                            "templates/Index/index.html")
                    .addAsResource(new StringAsset("hello=Ahoj {name}!"), "messages/msg_Views_Index_cs.properties"));

    @Test
    public void testBundle() {
        assertEquals("Hello world!",
                Views.Index.Templates.index("world").render());
        assertEquals("Ahoj svete!", Views.Index.Templates.index("svete")
                .setAttribute(MessageBundles.ATTRIBUTE_LOCALE, Locale.forLanguageTag("cs")).render());
    }

}
