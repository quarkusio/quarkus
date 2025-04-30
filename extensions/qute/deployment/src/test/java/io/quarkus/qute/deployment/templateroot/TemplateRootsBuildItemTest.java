package io.quarkus.qute.deployment.templateroot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.deployment.TemplateRootsBuildItem;

public class TemplateRootsBuildItemTest {

    @Test
    public void testIsRoot() {
        TemplateRootsBuildItem buildItem = new TemplateRootsBuildItem(Set.of("templates", "public/web"));
        assertTrue(buildItem.isRoot(Path.of("/templates")));
        assertTrue(buildItem.isRoot(Path.of("public/web")));
        assertTrue(buildItem.isRoot(Path.of("/templates/")));
        assertTrue(buildItem.isRoot(Path.of("public/web/")));
        assertFalse(buildItem.isRoot(Path.of("/foo/templates")));
        assertFalse(buildItem.isRoot(Path.of("/web")));
        assertFalse(buildItem.isRoot(Path.of("public")));
        assertFalse(buildItem.isRoot(Path.of("baz/web")));
        assertFalse(buildItem.isRoot(Path.of("baz/template")));
    }

    @Test
    public void testMaybeRoot() {
        TemplateRootsBuildItem buildItem = new TemplateRootsBuildItem(Set.of("templates", "public/web"));
        assertTrue(buildItem.maybeRoot(Path.of("public")));
        assertTrue(buildItem.maybeRoot(Path.of("templates")));
        assertTrue(buildItem.maybeRoot(Path.of(File.separatorChar + "public" + File.separatorChar)));
        assertFalse(buildItem.maybeRoot(Path.of("template")));
        assertFalse(buildItem.maybeRoot(Path.of("foo")));
    }
}
