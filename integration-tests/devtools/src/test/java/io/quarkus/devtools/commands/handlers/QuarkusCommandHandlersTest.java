package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.select;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.SelectionResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.json.JsonExtension;

class QuarkusCommandHandlersTest {

    @Test
    void testMultiMatchByLabels() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "e1", "1.0"));
        e1.setName("some extension 1");
        e1.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"));
        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "e2", "1.0"));
        e2.setName("some extension 2");
        e2.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar", "baz"));
        JsonExtension e3 = new JsonExtension();
        e3.setArtifact(new ArtifactCoords("org.acme", "e3", "1.0"));
        e3.setName("unrelated");
        e3.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("bar"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = select("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(0, matches.getExtensions().size());
    }

    @Test
    void testThatSingleLabelMatchIsNotAMatch() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "e1", "1.0"));
        e1.setName("e1");
        e1.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"));
        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "e2", "1.0"));
        e2.setName("e2");
        e2.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("bar", "baz"));

        List<Extension> extensions = asList(e1, e2);
        Collections.shuffle(extensions);
        SelectionResult matches = select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
    }

    @Test
    void testMultiMatchByArtifactIdsAndNames() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "e1", "1.0"));
        e1.setName("foo");
        e1.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar"));
        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo", "1.0"));
        e2.setName("some foo bar");
        e2.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        JsonExtension e3 = new JsonExtension();
        e3.setArtifact(new ArtifactCoords("org.acme", "e3", "1.0"));
        e3.setName("unrelated");
        e3.getMetadata().put(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = select("foo", extensions, false);
        Assertions.assertFalse(matches.matches(), " " + matches.getExtensions().size());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = select("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(3, matches.getExtensions().size());

    }

    @Test
    void testShortNameSelection() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "some-complex-seo-unaware-artifactid", "1.0"));
        e1.setName("some complex seo unaware name");
        e1.getMetadata().put(Extension.MD_SHORT_NAME, "foo");
        e1.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar"));
        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "some-foo-bar", "1.0"));
        e2.setName("some foo bar");
        e2.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        JsonExtension e3 = new JsonExtension();
        e3.setArtifact(new ArtifactCoords("org.acme", "unrelated", "1.0"));
        e3.setName("unrelated");
        e3.getMetadata().put(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = select("foo", extensions, false);
        Assertions.assertTrue(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions
                .assertTrue(matches.iterator().next().getArtifact().getArtifactId()
                        .equalsIgnoreCase("some-complex-seo-unaware-artifactid"));
    }

    @Test
    void testArtifactIdSelectionWithQuarkusPrefix() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo", "1.0"));
        e1.setName("some complex seo unaware name");
        e1.getMetadata().put(Extension.MD_SHORT_NAME, "foo");
        e1.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar"));
        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo-bar", "1.0"));
        e2.setName("some foo bar");
        e2.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        JsonExtension e3 = new JsonExtension();
        e3.setArtifact(new ArtifactCoords("org.acme", "quarkus-unrelated", "1.0"));
        e3.setName("unrelated");
        e3.getMetadata().put(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = select("foo", extensions, false);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions.assertTrue(matches.iterator().next().getArtifact().getArtifactId().equalsIgnoreCase("quarkus-foo"));
    }

    @Test
    void testListedVsUnlisted() {
        JsonExtension e1 = new JsonExtension();
        e1.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo-unlisted", "1.0"));
        e1.setName("some complex seo unaware name");
        e1.getMetadata().put(Extension.MD_SHORT_NAME, "foo");
        e1.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar"));
        e1.addMetadata("unlisted", "true");

        JsonExtension e2 = new JsonExtension();
        e2.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo-bar", "1.0"));
        e2.setName("some foo bar");
        e2.getMetadata().put(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        e2.addMetadata("unlisted", "false");
        JsonExtension e3 = new JsonExtension();
        e3.setArtifact(new ArtifactCoords("org.acme", "quarkus-foo-baz", "1.0"));
        e3.setName("unrelated");
        e3.getMetadata().put(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = select("quarkus-foo", extensions, true);
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = select("quarkus-foo-unlisted", extensions, true);
        Assertions.assertEquals(1, matches.getExtensions().size());

    }
}
