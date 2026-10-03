package io.quarkus.qute.deployment.globals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * User tags are isolated, so a global variable is only available in a tag, or in a template included from a tag,
 * through the {@code global:} namespace, unless the tag is called with {@code _unisolated}.
 */
public class TemplateGlobalInUserTagTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(Globals.class)
                    .addAsResource(new StringAsset("{year}"), "templates/tags/bareYear.txt")
                    .addAsResource(new StringAsset("{global:year}"), "templates/tags/namespaceYear.txt")
                    .addAsResource(new StringAsset("{#include footer /}"), "templates/tags/includeFooter.txt")
                    .addAsResource(new StringAsset("Year: {year}"), "templates/footer.txt")
                    .addAsResource(new StringAsset("{#bareYear /}"), "templates/bare.txt")
                    .addAsResource(new StringAsset("{#bareYear _unisolated /}"), "templates/unisolated.txt")
                    .addAsResource(new StringAsset("{#namespaceYear /}"), "templates/namespace.txt")
                    .addAsResource(new StringAsset("{#includeFooter /}"), "templates/includeFromTag.txt")
                    .addAsResource(new StringAsset("{#include footer /}"), "templates/includeFromTemplate.txt"));

    @Inject
    Template bare;

    @Inject
    Template unisolated;

    @Inject
    Template namespace;

    @Inject
    Template includeFromTag;

    @Inject
    Template includeFromTemplate;

    @Test
    public void globalIsNotAvailableInIsolatedTag() {
        TemplateException exception = assertThrows(TemplateException.class, () -> bare.render());
        assertTrue(exception.getMessage().contains("year"), exception.getMessage());
    }

    @Test
    public void globalIsAvailableThroughNamespaceInTag() {
        assertEquals("2026", namespace.render());
    }

    @Test
    public void globalIsAvailableInUnisolatedTag() {
        assertEquals("2026", unisolated.render());
    }

    @Test
    public void globalIsNotAvailableInTemplateIncludedFromIsolatedTag() {
        assertEquals("Year: 2026", includeFromTemplate.render());

        TemplateException exception = assertThrows(TemplateException.class, () -> includeFromTag.render());
        assertTrue(exception.getMessage().contains("year"), exception.getMessage());
    }

    public static class Globals {

        @TemplateGlobal
        static int year() {
            return 2026;
        }
    }
}
