package io.quarkus.qute.deployment.propertynotfound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class PropertyNotFoundNoopTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClass(Item.class)
                    .addAsResource(new StringAsset("foos:{foos}"), "templates/test.html")
                    .addAsResource(
                            new StringAsset(
                                    "name:{item.name} nonExistent:{item.nonExistent}"),
                            "templates/beantest.txt"))
            .overrideConfigKey("quarkus.qute.property-not-found-strategy", "noop")
            .overrideConfigKey("quarkus.qute.strict-rendering", "false");

    @Inject
    Template test;

    @Inject
    Template beantest;

    @Test
    public void testNoop() {
        // Missing top-level template data key should produce empty output
        assertEquals("foos:", test.render());
    }

    @Test
    public void testNoopBeanProperty() {
        // NOOP strategy should also apply to missing bean properties;
        // i.e. {item.nonExistent} should produce empty output, not "NOT_FOUND"
        assertEquals("name:Hello nonExistent:", beantest.data("item", new Item("Hello")).render());
    }

    public static class Item {

        public final String name;

        public Item(String name) {
            this.name = name;
        }
    }

}
