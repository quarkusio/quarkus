package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class DataNamespaceArrayValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Item.class, OtherItem.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Item item}\n"
                            + "{item.name}\n" + "  {#for item in item.otherItems}\n"
                            + "    {data:item.otherItems[0].name}\n" + "  {/for}\n"), "templates/item.html"))
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
                        "Property/method [name] not found on class [io.quarkus.qute.deployment.typesafe.OtherItem]"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

}
