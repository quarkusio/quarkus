package io.quarkus.qute.deployment.scanning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An application with tens of thousands of templates must build: the template list must not
 * be recorded as constants of one generated class.
 */
public class ManyTemplatesTest {

    static final int COUNT = 20_000;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                for (int i = 0; i < COUNT; i++) {
                    root.addAsResource(new StringAsset("Hello " + i + "!"), "templates/many/page-" + i + ".html");
                }
            });

    @Inject
    Engine engine;

    @Test
    public void testAllTemplatesFound() {
        assertEquals("Hello 0!", engine.getTemplate("many/page-0.html").render());
        assertEquals("Hello " + (COUNT - 1) + "!", engine.getTemplate("many/page-" + (COUNT - 1)).render());
    }

}
