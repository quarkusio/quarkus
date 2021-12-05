package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleExpressionValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WrongBundle.class, Item.class)
                    .addAsResource(new StringAsset(
                            "hello=Hallo {foo}!"),
                            "messages/msg_de.properties"))
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
                assertTrue(te.getMessage().contains("Found template problems (3)"), te.getMessage());
                assertTrue(te.getMessage().contains("item.foo"), te.getMessage());
                assertTrue(te.getMessage().contains("bar"), te.getMessage());
                assertTrue(te.getMessage().contains("foo"), te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface WrongBundle {

        @Message("Hello {item.foo} {bar}") // -> there is no foo property and bar is not a parameter 
        String hello(Item item);

    }

}
