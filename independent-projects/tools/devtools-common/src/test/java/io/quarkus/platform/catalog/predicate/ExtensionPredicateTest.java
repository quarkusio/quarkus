package io.quarkus.platform.catalog.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ExtensionPredicateTest {

    @Test
    void rejectUnlisted() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(new ArtifactCoords("g", "a", null, "jar", "v"))
                .setMetadata(Extension.MD_UNLISTED, true);
        assertThat(predicate).rejects(extension);
    }

    @Test
    void acceptKeywordInArtifactId() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(new ArtifactCoords("g", "foo-bar", null, "jar", "1.0"))
                .build();
        assertThat(predicate).accepts(extension);
    }

    @Test
    void acceptKeywordInLabel() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        Extension extension = Extension.builder()
                .setArtifact(new ArtifactCoords("g", "a", null, "jar", "1.0"))
                .setMetadata(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"))
                .build();
        assertThat(predicate).accepts(extension);
    }

}
