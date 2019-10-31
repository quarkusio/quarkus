package io.quarkus.cli.commands;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.dependencies.Extension;

class AddExtensionsSelectTest {

    @Test
    void testMultiMatchByLabels() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("some extension 1")
                .setKeywords(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "e2", "1.0")
                .setName("some extension 2")
                .setKeywords(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "e3", "1.0")
                .setName("unrelated")
                .setKeywords(new String[] { "bar" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(0, matches.getExtensions().size());
    }

    @Test
    void testThatSingleLabelMatchIsNotAMatch() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("e1")
                .setKeywords(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "e2", "1.0")
                .setName("e2")
                .setKeywords(new String[] { "bar", "baz" });

        List<Extension> extensions = asList(e1, e2);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
    }

    @Test
    void testMultiMatchByArtifactIdsAndNames() {
        Extension e1 = new Extension("org.acme", "e1", "1.0")
                .setName("foo")
                .setKeywords(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "quarkus-foo", "1.0")
                .setName("some foo bar")
                .setKeywords(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "e3", "1.0")
                .setName("unrelated")
                .setKeywords(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = AddExtensions.select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(3, matches.getExtensions().size());

    }

    @Test
    void testShortNameSelection() {
        Extension e1 = new Extension("org.acme", "some-complex-seo-unaware-artifactid", "1.0")
                .setName("some complex seo unaware name")
                .setShortName("foo")
                .setKeywords(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "some-foo-bar", "1.0")
                .setName("some foo bar")
                .setKeywords(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "unrelated", "1.0")
                .setName("unrelated")
                .setKeywords(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertTrue(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions
                .assertTrue(matches.iterator().next().getArtifactId().equalsIgnoreCase("some-complex-seo-unaware-artifactid"));
    }

    @Test
    void testArtifactIdSelectionWithQuarkusPrefix() {
        Extension e1 = new Extension("org.acme", "quarkus-foo", "1.0")
                .setName("some complex seo unaware name")
                .setShortName("foo")
                .setKeywords(new String[] { "foo", "bar" });
        Extension e2 = new Extension("org.acme", "quarkus-foo-bar", "1.0")
                .setName("some foo bar")
                .setKeywords(new String[] { "foo", "bar", "baz" });
        Extension e3 = new Extension("org.acme", "quarkus-unrelated", "1.0")
                .setName("unrelated")
                .setKeywords(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("foo", extensions, false);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions.assertTrue(matches.iterator().next().getArtifactId().equalsIgnoreCase("quarkus-foo"));
    }

    @Test
    void testListedVsUnlisted() {
        Extension e1 = new Extension("org.acme", "quarkus-foo-unlisted", "1.0")
                .setName("some complex seo unaware name")
                .setShortName("foo")
                .setKeywords(new String[] { "foo", "bar" }).addMetadata("unlisted", "true");
        
        Extension e2 = new Extension("org.acme", "quarkus-foo-bar", "1.0")
                .setName("some foo bar")
                .setKeywords(new String[] { "foo", "bar", "baz" }).addMetadata("unlisted", "false");
        Extension e3 = new Extension("org.acme", "quarkus-foo-baz", "1.0")
                .setName("unrelated")
                .setKeywords(new String[] { "foo" });

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = AddExtensions.select("quarkus-foo", extensions, true);
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = AddExtensions.select("quarkus-foo-unlisted", extensions, true);
        Assertions.assertEquals(1, matches.getExtensions().size());

    }
    
    
}
