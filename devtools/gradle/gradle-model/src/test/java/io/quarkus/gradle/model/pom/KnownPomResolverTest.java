package io.quarkus.gradle.model.pom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.dependency.GAV;

class KnownPomResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFailClosedForAmbiguousGradleCachePoms() throws IOException {
        installGradleCachePom("first-hash");
        installGradleCachePom("second-hash");
        GAV sample = new GAV("org.acme", "sample", "1.0");
        KnownPomResolver resolver = KnownPomResolver.fromPomClosure(
                Map.of(), Set.of(), List.of(tempDir.toFile()));

        assertThatThrownBy(() -> resolver.resolvePom(sample))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:sample:1.0");
    }

    private void installGradleCachePom(String hash) throws IOException {
        Path artifactDirectory = tempDir.resolve("org.acme").resolve("sample").resolve("1.0").resolve(hash);
        Files.createDirectories(artifactDirectory);
        Files.writeString(artifactDirectory.resolve("sample-1.0.pom"), "<project/>", StandardCharsets.UTF_8);
    }
}
