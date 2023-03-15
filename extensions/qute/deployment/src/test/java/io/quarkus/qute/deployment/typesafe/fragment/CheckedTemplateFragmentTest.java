package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateFragmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Templates.class, Item.class)
                    .addAsResource(new StringAsset(
                            "{#each items}{#fragment id='item'}{it.name}{#if it.name.length > 5} is a long name{/if}{/fragment}{/each}"),
                            "templates/CheckedTemplateFragmentTest/items.html")
                    .addAsResource(new StringAsset(
                            "{#fragment id=foo}{#for i in bar}{i_count}. <{i}>{#if i_hasNext}, {/if}{/for}{/fragment}"),
                            "templates/CheckedTemplateFragmentTest/foos.html"));

    @Test
    public void testFragment() {
        assertEquals("Foo", Templates.items(null).getFragment("item").data("it", new Item("Foo")).render());
        assertEquals("Foo", Templates.items$item(new Item("Foo")).render());
        assertEquals("FooAndBar is a long name", Templates.items$item(new Item("FooAndBar")).render());
        assertEquals("1. <1>, 2. <2>, 3. <3>, 4. <4>, 5. <5>", Templates.foos$foo().render());
    }

    @CheckedTemplate
    public static class Templates {

        @TemplateGlobal
        static int bar = 5;

        static native TemplateInstance items(List<Item> items);

        static native TemplateInstance items$item(Item it);

        static native TemplateInstance foos$foo();
    }

}
