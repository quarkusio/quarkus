package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateFragmentVariantTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(root -> root
            .addClasses(Templates.class, Item.class)
            .addAsResource(new StringAsset("{#each items}{#fragment id='item'}<p>{it.name}</p>{/fragment}{/each}"),
                    "templates/CheckedTemplateFragmentVariantTest/items.html")
            .addAsResource(new StringAsset("{#each items}{#fragment id='item'}{it.name}{/fragment}{/each}"),
                    "templates/CheckedTemplateFragmentVariantTest/items.txt"));

    @SuppressWarnings("unchecked")
    @Test
    public void testFragment() {
        TemplateInstance fragment = Templates.items$item(new Item("Foo"));
        List<Variant> variants = (List<Variant>) fragment.getAttribute(TemplateInstance.VARIANTS);
        assertEquals(2, variants.size());

        assertEquals("<p>Foo</p>",
                fragment.setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType("text/html")).render());
        assertEquals("Foo", fragment
                .setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType("text/plain")).render());
        // A variant for application/json does not exist, use the default - html wins
        assertEquals("<p>Foo</p>", fragment
                .setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType("application/json")).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance items(List<Item> items);

        static native TemplateInstance items$item(Item it);

    }

}
