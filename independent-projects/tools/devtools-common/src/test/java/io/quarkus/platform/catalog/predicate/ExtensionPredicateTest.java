package io.quarkus.platform.catalog.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.json.JsonExtension;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ExtensionPredicateTest {

    @Test
    void rejectUnlisted() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        JsonExtension extension = new JsonExtension();
        extension.setArtifact(new ArtifactCoords("g", "a", null, "jar", "v"));
        extension.getMetadata().put(Extension.MD_UNLISTED, true);
        assertThat(predicate).rejects(extension);
    }

    @Test
    void acceptKeywordInArtifactId() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        JsonExtension extension = new JsonExtension();
        extension.setArtifact(new ArtifactCoords("g", "foo-bar", null, "jar", "1.0"));
        assertThat(predicate).accepts(extension);
    }

    @Test
    void acceptKeywordInLabel() {
        ExtensionPredicate predicate = new ExtensionPredicate("foo");
        JsonExtension extension = new JsonExtension();
        extension.setArtifact(new ArtifactCoords("g", "a", null, "jar", "1.0"));
        extension.getMetadata().put(Extension.MD_KEYWORDS, Arrays.asList("foo", "bar"));
        assertThat(predicate).accepts(extension);
    }

}
