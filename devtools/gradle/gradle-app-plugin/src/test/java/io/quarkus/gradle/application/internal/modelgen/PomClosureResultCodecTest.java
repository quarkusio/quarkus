package io.quarkus.gradle.application.internal.modelgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.model.pom.PomClosureResult;
import io.quarkus.gradle.model.pom.PomClosureResultCodec;
import io.quarkus.maven.dependency.GAV;

class PomClosureResultCodecTest {

    @TempDir
    Path testDirectory;

    @Test
    void roundTripsResolvedAndMissingPomsWithoutTimestampComment() throws Exception {
        Path file = testDirectory.resolve("pom-closure.properties");
        GAV resolved = new GAV("org.acme", "resolved", "1.0");
        GAV missing = new GAV("org.acme", "missing", "1.0");
        Path pom = testDirectory.resolve("resolved.pom");

        PomClosureResultCodec.write(new PomClosureResult(Map.of(resolved, pom.toFile()), Set.of(missing)), file);

        assertThat(Files.readString(file)).doesNotContain("#");
        PomClosureResult decoded = PomClosureResultCodec.read(file);
        assertThat(decoded.resolvedPoms()).containsExactlyEntriesOf(Map.of(resolved, pom.toFile()));
        assertThat(decoded.missingPoms()).containsExactly(missing);
    }

    @Test
    void rejectsMalformedFiles() throws Exception {
        Path file = testDirectory.resolve("broken.properties");
        Files.writeString(file, "count=1\nentry.0.gav=broken\nentry.0.resolved=true\n");

        assertThatThrownBy(() -> PomClosureResultCodec.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("groupId:artifactId:version");
    }
}
