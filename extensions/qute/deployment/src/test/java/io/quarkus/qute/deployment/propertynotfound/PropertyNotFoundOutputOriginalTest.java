package io.quarkus.qute.deployment.propertynotfound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class PropertyNotFoundOutputOriginalTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClass(Item.class)
                    .addAsResource(new StringAsset("foos:{foos}"), "templates/test.html")
                    .addAsResource(
                            new StringAsset(
                                    "name:{item.name} nonExistent:{item.nonExistent}"),
                            "templates/beantest.txt"))
            .overrideConfigKey("quarkus.qute.property-not-found-strategy", "output-original")
            .overrideConfigKey("quarkus.qute.strict-rendering", "false");

    @Inject
    Template test;

    @Inject
    Template beantest;

    @Test
    public void testOriginal() {
        // Missing top-level template data key should output the original expression
        assertEquals("foos:{foos}", test.render());
    }

    @Test
    public void testOriginalBeanProperty() {
        // OUTPUT_ORIGINAL strategy should also apply to missing bean properties
        assertEquals("name:Hello nonExistent:{item.nonExistent}",
                beantest.data("item", new Item("Hello")).render());
    }

    public static class Item {

        public final String name;

        public Item(String name) {
            this.name = name;
        }
    }

}
