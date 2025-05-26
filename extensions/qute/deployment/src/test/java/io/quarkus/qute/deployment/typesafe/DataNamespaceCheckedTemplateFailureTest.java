package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class DataNamespaceCheckedTemplateFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class, Item.class, OtherItem.class)
                    .addAsResource(new StringAsset("Hello {data:item.unknownProperty}!"),
                            "templates/DataNamespaceCheckedTemplateFailureTest/greetings.txt"))
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
            });

    @Test
    public void test() {
        fail();
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance greetings(Item item);

    }

}
