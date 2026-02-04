package io.quarkus.bootstrap.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathTree;

/**
 * Tests for {@link LazySourceDir}, particularly verifying that non-existent
 * directories return {@link EmptyPathTree} instead of {@link io.quarkus.paths.DirectoryPathTree}.
 */
public class LazySourceDirTest {

    @TempDir
    Path tempDir;

    @Test
    public void getSourceTreeReturnsEmptyForNonExistentDir() {
        Path srcDir = tempDir.resolve("non-existent-src");
        Path destDir = tempDir.resolve("non-existent-dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        PathTree sourceTree = sourceDir.getSourceTree();
        assertThat(sourceTree).isInstanceOf(EmptyPathTree.class);
        assertThat(sourceTree.isEmpty()).isTrue();
    }

    @Test
    public void getOutputTreeReturnsEmptyForNonExistentDir() {
        Path srcDir = tempDir.resolve("non-existent-src");
        Path destDir = tempDir.resolve("non-existent-dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        PathTree outputTree = sourceDir.getOutputTree();
        assertThat(outputTree).isInstanceOf(EmptyPathTree.class);
        assertThat(outputTree.isEmpty()).isTrue();
    }

    @Test
    public void getSourceTreeReturnsDirectoryTreeForExistingDir() throws IOException {
        Path srcDir = tempDir.resolve("existing-src");
        Path destDir = tempDir.resolve("non-existent-dest");
        Files.createDirectories(srcDir);

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        PathTree sourceTree = sourceDir.getSourceTree();
        assertThat(sourceTree.isEmpty()).isFalse();
        assertThat(sourceTree.getRoots()).containsExactly(srcDir);
    }

    @Test
    public void getOutputTreeReturnsDirectoryTreeForExistingDir() throws IOException {
        Path srcDir = tempDir.resolve("non-existent-src");
        Path destDir = tempDir.resolve("existing-dest");
        Files.createDirectories(destDir);

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        PathTree outputTree = sourceDir.getOutputTree();
        assertThat(outputTree.isEmpty()).isFalse();
        assertThat(outputTree.getRoots()).containsExactly(destDir);
    }

    @Test
    public void isOutputAvailableReturnsFalseForNonExistentDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("non-existent-dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.isOutputAvailable()).isFalse();
    }

    @Test
    public void isOutputAvailableReturnsTrueForExistingDir() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("existing-dest");
        Files.createDirectories(destDir);

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.isOutputAvailable()).isTrue();
    }
}
