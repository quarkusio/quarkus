package io.quarkus.platform.catalog.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;

class ExtensionPredicateTest {

    @Test
    void rejectUnlisted() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(ArtifactCoords.jar("g", "a", "v"))
                .setMetadata(Extension.MD_UNLISTED, true);
        assertThat(predicate).rejects(extension);
    }

    @Test
    void acceptKeywordInArtifactId() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(ArtifactCoords.jar("g", "foo-bar", "1.0"))
                .build();
        assertThat(predicate).accepts(extension);
    }

    @Test
    void acceptKeywordInLabel() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(ArtifactCoords.jar("g", "a", "1.0"))
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"))
                .build();
        assertThat(predicate).accepts(extension);
    }

}
