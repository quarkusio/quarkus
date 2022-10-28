package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class OrOperatorTemplateExtensionTest {

    public static final String ITEM_NAME = "Test Name";
    public static final String ITEM_WITH_NAME = "itemWithName";
    public static final String ITEM = "item";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class, OtherItem.class, ItemWithName.class, ItemWithName.Name.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.ItemWithName itemWithName}\n" +
                                    "{@io.quarkus.qute.deployment.typesafe.Item item}\n" +
                                    "{#for otherItem in item.otherItems}\n" +
                                    "{missing.or(alsoMissing.or('item id is: ')).toLowerCase}" +
                                    "{item.name.or(itemWithName.name).toUpperCase}" +
                                    "{otherItem.id.or(itemWithName.id).longValue()}" +
                                    "{/for}\n"),
                            "templates/item.html"));

    @Inject
    Template item;

    @Test
    public void test() {
        final String expected = "item id is: " + ITEM_NAME.toUpperCase();
        final ItemWithName itemWithName = new ItemWithName(new ItemWithName.Name());

        // id comes from OtherItem, name is String and toUpperCase is method from String
        assertEquals(expected + OtherItem.ID,
                item.data(ITEM, new Item(ITEM_NAME.toUpperCase(), new OtherItem()), ITEM_WITH_NAME, itemWithName).render()
                        .trim());

        // id comes from ItemWithName, name comes from ItemWithName.Name and toUpperCase is regular method
        assertEquals(
                expected + itemWithName.getId(),
                item.data(ITEM, new Item(null, (OtherItem) null), ITEM_WITH_NAME, itemWithName).render().trim());
    }

}
