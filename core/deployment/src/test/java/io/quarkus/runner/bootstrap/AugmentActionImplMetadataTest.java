package io.quarkus.runner.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

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
    void missingValueOrOutputDirectory() {
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(OUTPUT_DIRECTORY, null)).isNull();
        String libraryDir = OUTPUT_DIRECTORY.resolve("lib").toString();
        assertThat(AugmentActionImpl.relativizeToOutputDirectory(null, libraryDir)).isEqualTo(libraryDir);
    }
}
