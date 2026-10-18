package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.dev.spi.DevModeType;

class RuntimeUpdatesProcessorUpdateFileTest {

    @TempDir
    Path applicationRoot;

    @SuppressWarnings("resource")
    @Test
    void writesNestedFile() {
        var processor = newProcessor();

        processor.updateFile("nested/file.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertThat(applicationRoot.resolve("nested/file.txt")).hasContent("content");

        processor.deleteFile("nested/file.txt");

        assertThat(applicationRoot.resolve("nested/file.txt")).doesNotExist();
    }

    @SuppressWarnings("resource")
    @Test
    void stripsSingleLeadingSlashBeforeWriting() {
        var processor = newProcessor();

        processor.updateFile("/nested/file.txt", "content".getBytes(StandardCharsets.UTF_8));

        assertThat(applicationRoot.resolve("nested/file.txt")).hasContent("content");

        processor.deleteFile("/nested/file.txt");

        assertThat(applicationRoot.resolve("nested/file.txt")).doesNotExist();
    }

    @SuppressWarnings("resource")
    @Test
    void nullHandling() {
        var processor = newProcessor();

        assertThatThrownBy(() -> processor.updateFile("nested/file.txt", null))
                .isInstanceOf(NullPointerException.class);
        assertThat(Files.exists(applicationRoot.resolve("nested/file.txt"))).isFalse();
        assertThatThrownBy(() -> processor.updateFile(null, "content".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> processor.deleteFile(null))
                .isInstanceOf(NullPointerException.class);
    }

    @SuppressWarnings("resource")
    @Test
    void rejectsWritesAndDeletesOutsideApplicationRoot() throws Exception {
        var processor = newProcessor();
        Path outside = applicationRoot.resolveSibling("outside.txt");
        Files.writeString(outside, "keep");

        assertThatThrownBy(() -> processor.updateFile("../outside.txt", "replace".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("../outside.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.updateFile("..\\outside.txt", "replace".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("..\\outside.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("C:outside.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("a:b/file.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("/")).isInstanceOf(IllegalArgumentException.class);

        assertThat(outside).hasContent("keep");
        assertThat(applicationRoot).isDirectory();
    }

    @SuppressWarnings("resource")
    @Test
    void rejectsSymbolicLinksBelowApplicationRoot() throws Exception {
        Path outside = applicationRoot.resolveSibling("outside-directory");
        Files.createDirectories(outside);
        Path link = applicationRoot.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            Assumptions.abort("Symbolic links are unavailable: " + e.getMessage());
        }
        Path outsideFile = outside.resolve("file.txt");
        Files.writeString(outsideFile, "keep");
        var processor = newProcessor();

        assertThatThrownBy(() -> processor.updateFile("link/file.txt", "replace".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.deleteFile("link/file.txt")).isInstanceOf(IllegalArgumentException.class);

        assertThat(outsideFile).hasContent("keep");
    }

    private RuntimeUpdatesProcessor newProcessor() {
        return new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL, null, null, null, null,
                new AtomicReference<>());
    }
}
