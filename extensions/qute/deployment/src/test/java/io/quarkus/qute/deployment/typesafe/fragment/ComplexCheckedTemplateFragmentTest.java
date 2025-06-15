package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class ComplexCheckedTemplateFragmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(Templates.class, Item.class).addAsResource(
                            new StringAsset("{#let foo=items}" + "{#fragment item_a}" + "{#let size=foo.size}"
                                    + "{#fragment id='item_b'}" + "{foo.first.name}::{size}" + "{/fragment}" + "{/let}"
                                    + "{/fragment}" + "{/let}"),
                            "templates/ComplexCheckedTemplateFragmentTest/items.html"));

    @Test
    public void testFragment() {
        assertEquals("Foo::10", Templates.items$item_b(List.of(new Item("Foo")), 10).render());
        assertEquals("Foo::1", Templates.items$item_a(List.of(new Item("Foo"))).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance items(List<Item> items);

        static native TemplateInstance items$item_b(List<Item> foo, int size);

        static native TemplateInstance items$item_a(List<Item> foo);
    }

}
