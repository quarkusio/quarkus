package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateIgnoreFragmentsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Templates.class, Item.class)
                    .addAsResource(new StringAsset("{#each items}{it.name}{/each}"),
                            "templates/CheckedTemplateIgnoreFragmentsTest/items.html")
                    .addAsResource(new StringAsset("{it.name}"),
                            "templates/CheckedTemplateIgnoreFragmentsTest/items$item.html"));

    @Test
    public void testFragment() {
        assertEquals("Foo", Templates.items(List.of(new Item("Foo"))).render());
        assertEquals("Foo", Templates.items$item(new Item("Foo")).render());
    }

    @CheckedTemplate(ignoreFragments = true)
    public static class Templates {

        static native TemplateInstance items(List<Item> items);

        static native TemplateInstance items$item(Item it);
    }

}
