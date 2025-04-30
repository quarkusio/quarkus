package io.quarkus.qute.deployment.templateroot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.deployment.TemplateRootBuildItem;

public class TemplateRootBuildItemTest {

    @Test
    public void testNormalizedName() {
        assertEquals("foo", new TemplateRootBuildItem("/foo/ ").getPath());
        assertEquals("foo/bar", new TemplateRootBuildItem("/foo/bar").getPath());
        assertEquals("baz", new TemplateRootBuildItem(" baz/").getPath());
    }

}
