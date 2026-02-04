package io.quarkus.bootstrap.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;

/**
 * Tests for {@link LazySourceDir}, verifying:
 * <ul>
 * <li>Non-existent directories return {@link EmptyPathTree}</li>
 * <li>Existing directories return {@link io.quarkus.paths.DirectoryPathTree}</li>
 * <li>Path getters return correct values</li>
 * <li>Serialization/deserialization (both JSON map and Java serialization)</li>
 * <li>PathFilter support</li>
 * <li>Generated sources directory handling</li>
 * <li>Data map access via getValue()</li>
 * </ul>
 */
public class LazySourceDirTest {

    @TempDir
    Path tempDir;

    // ========== Non-existent directory tests ==========

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
    public void isOutputAvailableReturnsFalseForNonExistentDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("non-existent-dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.isOutputAvailable()).isFalse();
    }

    // ========== Existing directory tests ==========

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
    public void isOutputAvailableReturnsTrueForExistingDir() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("existing-dest");
        Files.createDirectories(destDir);

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.isOutputAvailable()).isTrue();
    }

    // ========== Path getter tests ==========

    @Test
    public void getDirReturnsSrcDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.getDir()).isEqualTo(srcDir);
    }

    @Test
    public void getOutputDirReturnsDestDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.getOutputDir()).isEqualTo(destDir);
    }

    @Test
    public void getAptSourcesDirReturnsGeneratedSourcesDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");
        Path genSrcDir = tempDir.resolve("generated-sources");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir, genSrcDir);

        assertThat(sourceDir.getAptSourcesDir()).isEqualTo(genSrcDir);
    }

    @Test
    public void getAptSourcesDirReturnsNullWhenNotSet() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.getAptSourcesDir()).isNull();
    }

    // ========== Constructor null validation tests ==========

    @Test
    public void constructorRejectsSrcDirNull() {
        Path destDir = tempDir.resolve("dest");

        assertThatThrownBy(() -> new LazySourceDir(null, destDir))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("srcDir is null");
    }

    @Test
    public void constructorRejectsDestDirNull() {
        Path srcDir = tempDir.resolve("src");

        assertThatThrownBy(() -> new LazySourceDir(srcDir, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("destDir is null");
    }

    // ========== getValue() tests ==========

    @Test
    public void getValueReturnsDataFromMap() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");
        Map<Object, Object> data = Map.of("compiler", "javac", "version", 17);

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir, null, data);

        assertThat(sourceDir.getValue("compiler", String.class)).isEqualTo("javac");
        assertThat(sourceDir.getValue("version", Integer.class)).isEqualTo(17);
    }

    @Test
    public void getValueReturnsNullForMissingKey() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);

        assertThat(sourceDir.getValue("missing", String.class)).isNull();
    }

    // ========== JSON map serialization tests ==========

    @Test
    public void asMapContainsRequiredFields() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir);
        Map<String, Object> map = sourceDir.asMap(new TestCollectionFactory());

        assertThat(map).containsKey(BootstrapConstants.MAPPABLE_SRC_DIR);
        assertThat(map).containsKey(BootstrapConstants.MAPPABLE_DEST_DIR);
        assertThat(map.get(BootstrapConstants.MAPPABLE_SRC_DIR)).isEqualTo(srcDir.toString());
        assertThat(map.get(BootstrapConstants.MAPPABLE_DEST_DIR)).isEqualTo(destDir.toString());
    }

    @Test
    public void asMapContainsOptionalGenSrcDir() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");
        Path genSrcDir = tempDir.resolve("generated");

        LazySourceDir sourceDir = new LazySourceDir(srcDir, destDir, genSrcDir);
        Map<String, Object> map = sourceDir.asMap(new TestCollectionFactory());

        assertThat(map).containsKey(BootstrapConstants.MAPPABLE_APT_SOURCES_DIR);
        assertThat(map.get(BootstrapConstants.MAPPABLE_APT_SOURCES_DIR)).isEqualTo(genSrcDir.toString());
    }

    @Test
    public void fromMapRoundTripsCorrectly() {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");
        Path genSrcDir = tempDir.resolve("generated");

        LazySourceDir original = new LazySourceDir(srcDir, destDir, genSrcDir);
        Map<String, Object> map = original.asMap(new TestCollectionFactory());

        SourceDir restored = LazySourceDir.fromMap(map);

        assertThat(restored.getDir()).isEqualTo(srcDir);
        assertThat(restored.getOutputDir()).isEqualTo(destDir);
        assertThat(restored.getAptSourcesDir()).isEqualTo(genSrcDir);
    }

    @Test
    public void fromMapHandlesMissingGenSrcDir() {
        Map<String, Object> map = new HashMap<>();
        map.put(BootstrapConstants.MAPPABLE_SRC_DIR, tempDir.resolve("src").toString());
        map.put(BootstrapConstants.MAPPABLE_DEST_DIR, tempDir.resolve("dest").toString());

        SourceDir restored = LazySourceDir.fromMap(map);

        assertThat(restored.getAptSourcesDir()).isNull();
    }

    // ========== Java serialization tests ==========

    @Test
    public void javaSerializationRoundTripsCorrectly() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");
        Path genSrcDir = tempDir.resolve("generated");
        Files.createDirectories(srcDir);
        Files.createDirectories(destDir);

        LazySourceDir original = new LazySourceDir(srcDir, destDir, genSrcDir);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LazySourceDir restored;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            restored = (LazySourceDir) ois.readObject();
        }

        assertThat(restored.getDir()).isEqualTo(srcDir);
        assertThat(restored.getOutputDir()).isEqualTo(destDir);
        assertThat(restored.getAptSourcesDir()).isEqualTo(genSrcDir);
        // Trees should work after deserialization
        assertThat(restored.getSourceTree().isEmpty()).isFalse();
        assertThat(restored.getOutputTree().isEmpty()).isFalse();
    }

    @Test
    public void javaSerializationHandlesNullGenSrcDir() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Path destDir = tempDir.resolve("dest");

        LazySourceDir original = new LazySourceDir(srcDir, destDir);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LazySourceDir restored;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            restored = (LazySourceDir) ois.readObject();
        }

        assertThat(restored.getAptSourcesDir()).isNull();
    }

    // ========== PathFilter tests ==========

    @Test
    public void pathFilterIsAppliedToSourceTree() throws IOException {
        Path srcDir = tempDir.resolve("src-with-filter");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Include.java"), "class Include {}");
        Files.writeString(srcDir.resolve("Exclude.txt"), "excluded");

        // PathFilter uses glob patterns, not regex
        PathFilter filter = new PathFilter(List.of("*.java"), null);
        LazySourceDir sourceDir = new LazySourceDir(srcDir, filter, tempDir.resolve("dest"), null, null, Map.of());

        PathTree sourceTree = sourceDir.getSourceTree();
        assertThat(sourceTree.isEmpty()).isFalse();
        // The filter should be applied - only .java files visible
        assertThat(sourceTree.contains("Include.java")).isTrue();
        assertThat(sourceTree.contains("Exclude.txt")).isFalse();
    }

    @Test
    public void pathFilterIsAppliedToOutputTree() throws IOException {
        Path destDir = tempDir.resolve("dest-with-filter");
        Files.createDirectories(destDir);
        Files.writeString(destDir.resolve("Include.class"), "bytecode");
        Files.writeString(destDir.resolve("Exclude.txt"), "excluded");

        // PathFilter uses glob patterns, not regex
        PathFilter filter = new PathFilter(List.of("*.class"), null);
        LazySourceDir sourceDir = new LazySourceDir(tempDir.resolve("src"), null, destDir, filter, null, Map.of());

        PathTree outputTree = sourceDir.getOutputTree();
        assertThat(outputTree.isEmpty()).isFalse();
        // The filter should be applied - only .class files visible
        assertThat(outputTree.contains("Include.class")).isTrue();
        assertThat(outputTree.contains("Exclude.txt")).isFalse();
    }

    // ========== ArtifactSources integration tests ==========

    @Test
    public void artifactSourcesFiltersEmptyOutputTrees() throws IOException {
        Path existingSrc = tempDir.resolve("existing-src");
        Path existingDest = tempDir.resolve("existing-dest");
        Path nonExistentSrc = tempDir.resolve("non-existent-src");
        Path nonExistentDest = tempDir.resolve("non-existent-dest");
        Files.createDirectories(existingSrc);
        Files.createDirectories(existingDest);

        SourceDir existingSourceDir = new LazySourceDir(existingSrc, existingDest);
        SourceDir nonExistentSourceDir = new LazySourceDir(nonExistentSrc, nonExistentDest);

        ArtifactSources artifactSources = new DefaultArtifactSources(
                ArtifactSources.MAIN,
                List.of(existingSourceDir, nonExistentSourceDir),
                List.of());

        // isOutputAvailable should return true (at least one exists)
        assertThat(artifactSources.isOutputAvailable()).isTrue();

        // getOutputTree should only include existing directories
        PathTree outputTree = artifactSources.getOutputTree();
        assertThat(outputTree.isEmpty()).isFalse();
        assertThat(outputTree.getRoots()).hasSize(1);
        assertThat(outputTree.getRoots()).containsExactly(existingDest);
    }

    @Test
    public void artifactSourcesReturnsEmptyTreeWhenAllNonExistent() {
        Path nonExistentSrc1 = tempDir.resolve("non-existent-src1");
        Path nonExistentDest1 = tempDir.resolve("non-existent-dest1");
        Path nonExistentSrc2 = tempDir.resolve("non-existent-src2");
        Path nonExistentDest2 = tempDir.resolve("non-existent-dest2");

        SourceDir sourceDir1 = new LazySourceDir(nonExistentSrc1, nonExistentDest1);
        SourceDir sourceDir2 = new LazySourceDir(nonExistentSrc2, nonExistentDest2);

        ArtifactSources artifactSources = new DefaultArtifactSources(
                ArtifactSources.MAIN,
                List.of(sourceDir1, sourceDir2),
                List.of());

        assertThat(artifactSources.isOutputAvailable()).isFalse();
        assertThat(artifactSources.getOutputTree()).isInstanceOf(EmptyPathTree.class);
    }

    // ========== Helper classes ==========

    private static class TestCollectionFactory implements MappableCollectionFactory {
        @Override
        public Map<String, Object> newMap() {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> newMap(int size) {
            return new HashMap<>(size);
        }

        @Override
        public java.util.Collection<Object> newCollection() {
            return new java.util.ArrayList<>();
        }

        @Override
        public java.util.Collection<Object> newCollection(int size) {
            return new java.util.ArrayList<>(size);
        }
    }
}
