package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

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

    private RuntimeUpdatesProcessor newProcessor() {
        return new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL, null, null, null, null,
                new AtomicReference<>());
    }
}
