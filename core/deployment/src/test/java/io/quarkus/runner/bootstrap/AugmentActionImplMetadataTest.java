package io.quarkus.runner.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AugmentActionImplMetadataTest {

    private static final Path OUTPUT_DIRECTORY = Path.of("checkout", "build").toAbsolutePath();

    @Test
    void locationUnderTheOutputDirectoryIsRelative() {
        String libraryDir = OUTPUT_DIRECTORY.resolve("quarkus-app").resolve("lib").toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
    }

    @Test
    void absoluteLocationElsewhereIsUnchanged() {
        String elsewhere = Path.of("elsewhere", "lib").toAbsolutePath().toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, elsewhere)).isEqualTo(elsewhere);
    }

    @Test
    void siblingWithTheSamePrefixIsUnchanged() {
        String sibling = OUTPUT_DIRECTORY.resolveSibling("build-2").resolve("lib").toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, sibling)).isEqualTo(sibling);
    }

    @Test
    void locationThatTraversesOutOfTheOutputDirectoryIsUnchanged() {
        String outside = OUTPUT_DIRECTORY.resolve("..").resolve("elsewhere").resolve("lib").toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, outside)).isEqualTo(outside);
    }

    @Test
    void locationThatIsNotNormalizedIsRelative() {
        String libraryDir = OUTPUT_DIRECTORY.resolve(".").resolve("quarkus-app").resolve("tmp").resolve("..")
                .resolve("lib").toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
    }

    @Test
    void outputDirectoryThatIsRelativeOrNotNormalized() {
        String libraryDir = OUTPUT_DIRECTORY.resolve("quarkus-app").resolve("lib").toString();

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(Path.of("checkout", "build"), libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY.resolve("tmp").resolve(".."), libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
    }

    @Test
    void outputDirectoryItselfIsTheCurrentDirectory() {
        // container image builds record it as output-directory
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, OUTPUT_DIRECTORY.toString()))
                .isEqualTo(".");
    }

    @Test
    void relativeValueIsUnchanged() {
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, "quarkus-app/lib"))
                .isEqualTo("quarkus-app/lib");
    }

    @Test
    void valueThatIsNotAPathIsUnchanged() {
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, "jvm")).isEqualTo("jvm");
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, "\0")).isEqualTo("\0");
    }

    @Test
    void containerImageMetadataIsUnchanged() {
        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "container-image", "quay.io/acme/app:1.0"))
                .isEqualTo("quay.io/acme/app:1.0");
        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "pull-required", "false")).isEqualTo("false");
        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "working-directory", "/home/jboss"))
                .isEqualTo("/home/jboss");
    }

    @Test
    void onlyTheLibraryAndOutputDirectoriesAreRelative() {
        String libraryDir = OUTPUT_DIRECTORY.resolve("quarkus-app").resolve("lib").toString();

        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "library-dir", libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "output-directory", OUTPUT_DIRECTORY.toString()))
                .isEqualTo(".");
        assertThat(AugmentActionImpl.metadataValue(OUTPUT_DIRECTORY, "other-location", libraryDir)).isEqualTo(libraryDir);
    }

    @Test
    void emptyValueIsUnchanged() {
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, "")).isEqualTo("");
    }

    @Test
    void locationWithATrailingSeparatorIsRelative() {
        String libraryDir = OUTPUT_DIRECTORY.resolve("quarkus-app").resolve("lib") + File.separator;

        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, libraryDir))
                .isEqualTo(Path.of("quarkus-app", "lib").toString());
    }

    @Test
    void missingValueOrOutputDirectory() {
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, null)).isNull();
        String libraryDir = OUTPUT_DIRECTORY.resolve("lib").toString();
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(null, libraryDir)).isEqualTo(libraryDir);
    }
}
