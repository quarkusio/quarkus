package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusUnitTest;

public class DataNamespaceValidationTest {

    private static final String ITEM_NAME = "Test Name";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class, OtherItem.class, Globals.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.Item item}\n" +
                                    "{#for item in item.otherItems}\n" +
                                    "  {data:item.name}\n" +
                                    "{/for}\n"),
                            "templates/item.html"));

    @Inject
    Template item;

    @Test
    public void testCorrectParamDeclarationIsAssumed() {
        // succeed as global item declaration is overridden
        assertEquals(
                ITEM_NAME,
                item.data("item", new Item(ITEM_NAME, new OtherItem())).render().trim());
    }

    public static class Globals {

        @TemplateGlobal
        static String item = "Item";

    }

}
