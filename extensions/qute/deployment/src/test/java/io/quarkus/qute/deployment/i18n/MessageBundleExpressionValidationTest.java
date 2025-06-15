package io.quarkus.qute.deployment.i18n;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleExpressionValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(WrongBundle.class, Item.class, MyGlobals.class)
                    .addAsResource(new StringAsset(
                            // foo is not a parameter of WrongBundle.hello()
                            "hello=Hallo {foo}!"), "messages/msg_de.properties"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                if (te == null) {
                    fail("No template exception thrown: " + t);
                }
                assertTrue(te.getMessage().contains("Found incorrect expressions (5)"), te.getMessage());
                assertTrue(te.getMessage().contains("item.foo"), te.getMessage());
                assertTrue(te.getMessage().contains("bar"), te.getMessage());
                assertTrue(te.getMessage().contains("foo"), te.getMessage());
                assertTrue(te.getMessage().contains("baf"), te.getMessage());
                assertTrue(te.getMessage().contains("it.baz"), te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle(DEFAULT_NAME)
    public interface WrongBundle {

        // item has no "foo" property, "bar" and "baf" are not parameters, string has no "baz" property
        @Message("Hello {item.foo} {bar} {#each item.names}{it}{it.baz}{it_hasNext}{baf}{/each}{level}")
        String hello(Item item);

    }

    @TemplateGlobal
    static class MyGlobals {

        static int level = 5;

    }

}
