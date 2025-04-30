package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleDefaultedNameTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Controller.class, AppMessages.class, AlphaMessages.class, Item.class)
                    .addAsResource(new StringAsset(
                            "{Controller_index:hello(name)}"),
                            "templates/Controller/index.html")
                    .addAsResource(new StringAsset(
                            "{msg:hello}"),
                            "templates/app.html")
                    .addAsResource(new StringAsset(
                            "{alpha:hello-alpha}"),
                            "templates/alpha.html")
                    .addAsResource(new StringAsset(
                            "{msg2:helloQux}"),
                            "templates/qux.html")
                    .addAsResource(new StringAsset("hello=Ahoj {name}!"), "messages/Controller_index_cs.properties"));

    @Inject
    Engine engine;

    @Test
    public void testBundles() {
        assertEquals("Hello world!",
                Controller.Templates.index("world").render());
        assertEquals("Ahoj svete!", Controller.Templates.index("svete").setLocale("cs").render());

        assertEquals("Hello world!", engine.getTemplate("app").render());
        assertEquals("Hello alpha!", engine.getTemplate("alpha").render());
        assertEquals("Hello qux!", engine.getTemplate("qux").render());
    }

    @MessageBundle("msg2")
    public interface MyAppMessages {

        @Message("Hello qux!")
        String helloQux();
    }

}
