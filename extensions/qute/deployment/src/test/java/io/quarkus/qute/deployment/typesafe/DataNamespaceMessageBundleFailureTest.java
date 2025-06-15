package io.quarkus.qute.deployment.typesafe;

import static io.quarkus.qute.i18n.MessageBundle.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class DataNamespaceMessageBundleFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addClasses(Hellos.class, Item.class, OtherItem.class, GoodByes.class).addAsResource(
                            new StringAsset("hello=Hallo {data:item.unknownProperty}!"), "messages/msg_de.properties"))
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
                assertNotNull(te);
                assertTrue(te.getMessage().contains(
                        "Property/method [unknownProperty] not found on class [io.quarkus.qute.deployment.typesafe.Item] nor handled by an extension method"),
                        te.getMessage());
                assertTrue(te.getMessage().contains(
                        "Property/method [missingProperty] not found on class [io.quarkus.qute.deployment.typesafe.Item] nor handled by an extension method"),
                        te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle(value = DEFAULT_NAME)
    public interface Hellos {

        @Message("Hello {data:item.name}")
        String hello(Item item);

    }

    @MessageBundle("Goodbyes")
    public interface GoodByes {

        @Message("Goodbye {data:item.missingProperty}")
        String goodbye(Item item);

    }

}
