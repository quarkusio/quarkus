package io.quarkus.qute.deployment.propertynotfound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusExtensionTest;

public class PropertyNotFoundThrowExceptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClass(Item.class)
                    .addAsResource(new StringAsset("foos:{foos}"), "templates/test.html")
                    .addAsResource(
                            new StringAsset("{item.nonExistent}"),
                            "templates/beantest.txt"))
            .overrideConfigKey("quarkus.qute.property-not-found-strategy", "throw-exception")
            .overrideConfigKey("quarkus.qute.strict-rendering", "false");

    @Inject
    Template test;

    @Inject
    Template beantest;

    @Test
    public void testException() {
        try {
            test.render();
            fail();
        } catch (Exception expected) {
            Throwable rootCause = ExceptionUtil.getRootCause(expected);
            assertEquals(TemplateException.class, rootCause.getClass());
            assertTrue(rootCause.getMessage().contains("Key \"foos\" not found in the template data map with keys []"),
                    rootCause.getMessage());
        }
    }

    @Test
    public void testExceptionBeanProperty() {
        // THROW_EXCEPTION strategy should also apply to missing bean properties
        Exception expected = assertThrows(Exception.class,
                () -> beantest.data("item", new Item("Hello")).render());
        Throwable rootCause = ExceptionUtil.getRootCause(expected);
        assertInstanceOf(TemplateException.class, rootCause);
        assertTrue(rootCause.getMessage().contains("Property \"nonExistent\" not found on the base object"),
                rootCause.getMessage());
    }

    public static class Item {

        public final String name;

        public Item(String name) {
            this.name = name;
        }
    }

}
