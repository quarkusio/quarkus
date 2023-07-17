package io.quarkus.qute.deployment.exclude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class TemplatePathExcludeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    // excluded
                    .addAsResource(new StringAsset("{@java.util.List myList}{myList.bar}"), "templates/.foo.txt")
                    .addAsResource(new StringAsset("{@java.util.List myList}{myList.bar}"), "templates/foo/bar/.hidden")
                    // not excluded
                    .addAsResource(new StringAsset("{@java.util.List myList}{myList.size}"), "templates/_foo.txt"));

    @Inject
    Engine engine;

    @Test
    public void testDefaults() {
        assertNull(engine.getTemplate(".foo.txt"));
        assertNull(engine.getTemplate("foo/bar/.hidden"));
        assertEquals(engine.getTemplate("_foo.txt").data("myList", List.of(1)).render(), "1");
    }

}
