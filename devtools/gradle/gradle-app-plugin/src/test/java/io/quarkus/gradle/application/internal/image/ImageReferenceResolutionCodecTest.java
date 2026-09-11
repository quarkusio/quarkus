package io.quarkus.gradle.application.internal.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageReferenceResolutionCodecTest {

    @TempDir
    Path directory;

    @Test
    void roundTripsPrimaryAndDeduplicatedAdditionalReferences() {
        Path receipt = directory.resolve("resolution.properties");
        ImageReferenceResolution resolution = new ImageReferenceResolution(
                "quay.io/acme/app:1", List.of("quay.io/acme/app:latest", "quay.io/acme/app:latest"));

        new ImageReferenceResolutionCodec().write(receipt, resolution);

        assertThat(new ImageReferenceResolutionCodec().read(receipt)).isEqualTo(
                new ImageReferenceResolution("quay.io/acme/app:1", List.of("quay.io/acme/app:latest")));
        assertThat(receipt).content().contains(
                "schema.version=1",
                "image.primary=quay.io/acme/app\\:1",
                "image.additional.count=1",
                "image.additional.0=quay.io/acme/app\\:latest");
        assertThat(resolution.additionalReferences()).containsExactly("quay.io/acme/app:latest");
    }

    @Test
    void rejectsBlankReferencesAndMalformedReceipts() throws Exception {
        assertThatThrownBy(() -> new ImageReferenceResolution(" ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary reference");
        assertThatThrownBy(() -> new ImageReferenceResolution("app:1", java.util.Arrays.asList((String) null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("additional reference");

        Path unsupported = directory.resolve("unsupported.properties");
        Files.writeString(unsupported, """
                schema.version=2
                image.primary=app:1
                image.additional.count=0
                """);
        assertThatThrownBy(() -> new ImageReferenceResolutionCodec().read(unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported schema version");

        Path unknown = directory.resolve("unknown.properties");
        Files.writeString(unknown, """
                schema.version=1
                image.primary=app:1
                image.additional.count=0
                secret=must-not-be-accepted
                """);
        assertThatThrownBy(() -> new ImageReferenceResolutionCodec().read(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown field 'secret'");

        Path duplicate = directory.resolve("duplicate.properties");
        Files.writeString(duplicate, """
                schema.version=1
                image.primary=app:1
                image.primary=app:2
                image.additional.count=0
                """);
        assertThatThrownBy(() -> new ImageReferenceResolutionCodec().read(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate field 'image.primary'");
    }
}
