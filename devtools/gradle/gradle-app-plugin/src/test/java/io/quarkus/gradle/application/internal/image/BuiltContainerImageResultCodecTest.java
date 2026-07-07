package io.quarkus.gradle.application.internal.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

class BuiltContainerImageResultCodecTest {

    private final BuiltContainerImageResultCodec codec = new BuiltContainerImageResultCodec();

    @TempDir
    Path directory;

    @Test
    void writesDeterministicPropertiesWithoutTimestamp() throws Exception {
        Path receipt = directory.resolve("image/result.properties");

        codec.write(receipt, image());

        assertThat(Files.readString(receipt)).isEqualTo("""
                image.builder=jib
                image.digest=sha256\\:abc
                image.id=image-id
                image.pushed=true
                image.reference=quay.io/acme/app\\:1.0
                image.working-directory=/work
                result.type=jar-container
                schema.version=1
                """);
    }

    @Test
    void roundTripsKnownFieldsAndOptionalAbsence() {
        Path receipt = directory.resolve("image/result.properties");
        var image = new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.DOCKER), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(false), Optional.empty(),
                Optional.of("target"));

        codec.write(receipt, image);

        assertThat(codec.read(receipt)).isEqualTo(image);
    }

    @Test
    void roundTripsAbsentBuilder() {
        Path receipt = directory.resolve("image/result.properties");
        var image = new BuiltContainerImage("jar-container", Optional.empty(), false,
                Optional.of("quay.io/acme/app:1.0"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        codec.write(receipt, image);

        assertThat(codec.read(receipt)).isEqualTo(image);
    }

    @Test
    void ignoresUnknownFieldsAndDoesNotInventDigest() throws Exception {
        Path receipt = directory.resolve("unknown.properties");
        Files.writeString(receipt, """
                schema.version=1
                result.type=jar-container
                image.builder=docker
                image.pushed=true
                image.reference=quay.io/acme/app:1.0
                image.unknown=value
                """);

        BuiltContainerImage image = codec.read(receipt);

        assertThat(image.reference()).contains("quay.io/acme/app:1.0");
        assertThat(image.digest()).isEmpty();
    }

    @Test
    void rejectsInvalidBooleans() throws Exception {
        Path receipt = directory.resolve("bad.properties");
        Files.writeString(receipt, """
                schema.version=1
                result.type=jar-container
                image.builder=jib
                image.pushed=maybe
                """);

        assertThatThrownBy(() -> codec.read(receipt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image.pushed")
                .hasMessageContaining("maybe");
    }

    @Test
    void rejectsMissingSchemaVersion() throws Exception {
        Path receipt = directory.resolve("missing-schema.properties");
        Files.writeString(receipt, """
                result.type=jar-container
                image.builder=jib
                image.pushed=true
                """);

        assertThatThrownBy(() -> codec.read(receipt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schema.version");
    }

    @Test
    void rejectsUnsupportedSchemaVersion() throws Exception {
        Path receipt = directory.resolve("unsupported-schema.properties");
        Files.writeString(receipt, """
                schema.version=2
                result.type=jar-container
                image.builder=jib
                image.pushed=true
                """);

        assertThatThrownBy(() -> codec.read(receipt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported schema version")
                .hasMessageContaining("2");
    }

    private static BuiltContainerImage image() {
        return new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.JIB), true,
                Optional.of("quay.io/acme/app:1.0"), Optional.of("sha256:abc"), Optional.of("image-id"),
                Optional.empty(), Optional.of("/work"), Optional.empty());
    }
}
