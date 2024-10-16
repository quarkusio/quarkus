package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.listExtensions;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.selectExtensions;
import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.SelectionResult;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;

class QuarkusCommandHandlersTest {

    @Test
    void testMultiMatchByLabels() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e1", "1.0"))
                .setName("some extension 1")
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"));

        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e2", "1.0"))
                .setName("some extension 2")
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar", "baz"));

        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e3", "1.0"))
                .setName("unrelated")
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("bar"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = selectExtensions("foo", extensions, false);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(0, matches.getExtensions().size());
    }

    @Test
    void testThatSingleLabelMatchIsNotAMatch() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e1", "1.0"))
                .setName("e1")
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"));

        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e2", "1.0"))
                .setName("e2")
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("bar", "baz"));

        List<Extension> extensions = asList(e1, e2);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
    }

    @Test
    void testMultiMatchByArtifactIdsAndNames() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e1", "1.0"))
                .setName("foo")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar"));
        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo", "1.0"))
                .setName("some foo bar")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "e3", "1.0"))
                .setName("unrelated")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("foo", extensions, false);
        Assertions.assertFalse(matches.matches(), " " + matches.getExtensions().size());
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = selectExtensions("foo", extensions, true);
        Assertions.assertFalse(matches.matches());
        Assertions.assertEquals(3, matches.getExtensions().size());

    }

    @Test
    void testShortNameSelection() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "some-complex-seo-unaware-artifactid", "1.0"))
                .setName("some complex seo unaware name")
                .setMetadata(Extension.MD_SHORT_NAME, "foo")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar"));
        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "some-foo-bar", "1.0"))
                .setName("some foo bar")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "unrelated", "1.0"))
                .setName("unrelated")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("foo", extensions, false);
        Assertions.assertTrue(matches.matches());
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions
                .assertTrue(matches.iterator().next().getArtifact().getArtifactId()
                        .equalsIgnoreCase("some-complex-seo-unaware-artifactid"));
    }

    @Test
    void testArtifactIdSelectionWithQuarkusPrefix() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo", "1.0"))
                .setName("some complex seo unaware name")
                .setMetadata(Extension.MD_SHORT_NAME, "foo")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar"));
        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo-bar", "1.0"))
                .setName("some foo bar")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"));
        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-unrelated", "1.0"))
                .setName("unrelated")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("foo", extensions, false);
        Assertions.assertEquals(1, matches.getExtensions().size());
        Assertions.assertTrue(matches.iterator().hasNext());
        Assertions.assertTrue(matches.iterator().next().getArtifact().getArtifactId().equalsIgnoreCase("quarkus-foo"));
    }

    @Test
    void testList() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-rest", "1.0"))
                .setName("Quarkus REST");

        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-rest-jackson", "1.0"))
                .setName("Quarkus REST Jackson");

        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-kafka", "1.0"))
                .setName("unrelated");

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("rest", extensions, true);
        Assertions.assertEquals(1, matches.getExtensions().size());

        matches = listExtensions("rest", extensions, true);
        Assertions.assertEquals(2, matches.getExtensions().size());
    }

    @Test
    void testListedVsUnlisted() {
        Extension e1 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo-unlisted", "1.0"))
                .setName("some complex seo unaware name")
                .setMetadata(Extension.MD_SHORT_NAME, "foo")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar"))
                .setMetadata("unlisted", "true");

        Extension e2 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo-bar", "1.0"))
                .setName("some foo bar")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo", "bar", "baz"))
                .setMetadata("unlisted", "false");

        Extension e3 = Extension.builder()
                .setArtifact(ArtifactCoords.jar("org.acme", "quarkus-foo-baz", "1.0"))
                .setName("unrelated")
                .setMetadata(Extension.MD_KEYWORDS, asList("foo"));

        List<Extension> extensions = asList(e1, e2, e3);
        Collections.shuffle(extensions);
        SelectionResult matches = selectExtensions("quarkus-foo", extensions, true);
        Assertions.assertEquals(2, matches.getExtensions().size());

        matches = selectExtensions("quarkus-foo-unlisted", extensions, true);
        Assertions.assertEquals(1, matches.getExtensions().size());

    }
}
