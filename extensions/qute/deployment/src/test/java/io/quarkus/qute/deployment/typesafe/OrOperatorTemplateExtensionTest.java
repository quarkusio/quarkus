package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class OrOperatorTemplateExtensionTest {

    public static final String ITEM_NAME = "Test Name";
    public static final String ITEM_WITH_NAME = "itemWithName";
    public static final String ITEM = "item";
    public static final String ITEMS = "items";
    public static final String ITEMS_2 = "items2";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class, OtherItem.class, ItemWithName.class, ItemWithName.Name.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.ItemWithName itemWithName}\n" +
                                    "{@io.quarkus.qute.deployment.typesafe.Item item}\n" +
                                    "{@io.quarkus.qute.deployment.typesafe.Item[] items}\n" +
                                    "{@io.quarkus.qute.deployment.typesafe.Item[] items2}\n" +
                                    "{#for otherItem in item.otherItems}\n" +
                                    "{missing.or(alsoMissing.or('result is: ')).toLowerCase}" +
                                    "{item.name.or(itemWithName.name).toUpperCase}" +
                                    "{items.or(items2).length}" + // test arrays
                                    "{otherItem.id.or(itemWithName.id).longValue()}" + // tests boxed type
                                    "{otherItem.getPrimitiveId().or(itemWithName.getPrimitiveId()).longValue()}" + // tests primitive type
                                    "{/for}\n"),
                            "templates/item.html"));

    @Inject
    Template item;

    @Test
    public void test() {
        final String expected = "result is: " + ITEM_NAME.toUpperCase();
        final ItemWithName itemWithName = new ItemWithName(new ItemWithName.Name());

        // ids comes from OtherItem, name is String and toUpperCase is method from String
        Item[] items = new Item[4];
        assertEquals(expected + items.length + OtherItem.ID + OtherItem.PRIMITIVE_ID,
                item.data(ITEM, new Item(ITEM_NAME.toUpperCase(), new OtherItem()), ITEM_WITH_NAME, itemWithName,
                        ITEMS, items, ITEMS_2, null).render()
                        .trim());

        // ids comes from ItemWithName, name comes from ItemWithName.Name and toUpperCase is regular method
        Item[] items2 = new Item[2];
        assertEquals(
                expected + items2.length + itemWithName.getId() + itemWithName.getPrimitiveId(),
                item.data(ITEM, new Item(null, (OtherItem) null), ITEM_WITH_NAME, itemWithName,
                        ITEMS, null, ITEMS_2, items2).render().trim());
    }

}
