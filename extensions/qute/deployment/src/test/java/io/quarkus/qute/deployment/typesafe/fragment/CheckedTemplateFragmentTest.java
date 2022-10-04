package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateFragmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Templates.class, Item.class)
                    .addAsResource(new StringAsset("{#each items}{#fragment id='item'}{it.name}{/fragment}{/each}"),
                            "templates/CheckedTemplateFragmentTest/items.html"));

    @Test
    public void testFragment() {
        assertEquals("Foo", Templates.items(null).getFragment("item").data("it", new Item("Foo")).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance items(List<Item> items);

    }

    public static class Item {

        String name;

        public Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
