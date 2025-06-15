package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class OrOperatorTemplateExtensionFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Item.class, OtherItem.class, ItemWithName.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Item item2}\n"
                            + "{#for item in item.otherItems}\n" + "{@io.quarkus.qute.deployment.typesafe.Item item}\n"
                            + "  {data:item.name.or(item2.name).pleaseMakeMyCaseUpper}\n"
                            + "  {item.name.or(item2.name).pleaseMakeMyCaseUpper}\n"
                            + "  {item.getPrimitiveId().or(item2.getPrimitiveId()).missingMethod()}\n" + "{/for}\n"),
                            "templates/item.html"))
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
                        "{item.name.or(item2.name).pleaseMakeMyCaseUpper}: Property/method [pleaseMakeMyCaseUpper] not found on class [java.lang.String]"),
                        te.getMessage());
                // validation failure in data namespace
                assertTrue(te.getMessage().contains(
                        "{data:item.name.or(item2.name).pleaseMakeMyCaseUpper}: Property/method [pleaseMakeMyCaseUpper] not found on class [java.lang.String]"),
                        te.getMessage());
                assertTrue(te.getMessage().contains(
                        "{item.getPrimitiveId().or(item2.getPrimitiveId()).missingMethod()}: Property/method [missingMethod()]"),
                        te.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
