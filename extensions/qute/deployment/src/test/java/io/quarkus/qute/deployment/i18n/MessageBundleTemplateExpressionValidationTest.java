package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleTemplateExpressionValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyBundle.class, Item.class)
                    .addAsResource(new StringAsset("{msg:hello('foo')} {msg:hello_and_bye} {msg:hello(1,2)}"),
                            "templates/hello.html"))
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
                assertTrue(te.getMessage().contains("msg:hello('foo')"), te.getMessage());
                assertTrue(te.getMessage().contains("msg:hello_and_bye"), te.getMessage());
                assertTrue(te.getMessage().contains("msg:hello(1,2)"), te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface MyBundle {

        @Message("Hello {item.name}")
        String hello(Item item);

    }

}
