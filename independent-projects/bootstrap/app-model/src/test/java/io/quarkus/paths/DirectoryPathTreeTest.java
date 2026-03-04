package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DirectoryPathTreeTest {

    private static final String BASE_DIR = "paths/directory-path-tree";

    private static volatile Path baseDir;

    static Map<String, String> getRawPaths() {
        final Map<String, String> expectedVisited = new HashMap<>();
        expectedVisited.put("", "");
        expectedVisited.put("README.md", "README.md");
        expectedVisited.put("META-INF", "META-INF");
        expectedVisited.put("META-INF/MANIFEST.MF", "META-INF/MANIFEST.MF");
        expectedVisited.put("META-INF/versions", "META-INF/versions");
        expectedVisited.put("META-INF/versions/9", "META-INF/versions/9");
        expectedVisited.put("META-INF/versions/9/org", "META-INF/versions/9/org");
        expectedVisited.put("META-INF/versions/9/org/acme", "META-INF/versions/9/org/acme");
        expectedVisited.put("META-INF/versions/9/org/acme/resource.txt", "META-INF/versions/9/org/acme/resource.txt");
        expectedVisited.put("org", "org");
        expectedVisited.put("org/acme", "org/acme");
        expectedVisited.put("org/acme/resource.txt", "org/acme/resource.txt");
        expectedVisited.put("src", "src");
        expectedVisited.put("src/main", "src/main");
        expectedVisited.put("src/main/java", "src/main/java");
        expectedVisited.put("src/main/java/Main.java", "src/main/java/Main.java");
        return expectedVisited;
    }

    static Map<String, String> getMultiReleaseMappedPaths() {
        final Map<String, String> expectedVisited = getRawPaths();
        expectedVisited.put("org", "META-INF/versions/9/org");
        expectedVisited.put("org/acme", "META-INF/versions/9/org/acme");
        expectedVisited.put("org/acme/resource.txt", "META-INF/versions/9/org/acme/resource.txt");
        return expectedVisited;
    }

    @BeforeAll
    public static void staticInit() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(BASE_DIR);
        if (url == null) {
            throw new IllegalStateException("Failed to locate " + BASE_DIR + " on the classpath");
        }
        baseDir = Path.of(url.toURI()).toAbsolutePath();
        if (!Files.exists(baseDir)) {
            throw new IllegalStateException("Failed to locate " + baseDir);
        }
    }

    @Test
    public void acceptExistingPath() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        tree.accept("README.md", visit -> {
            assertThat(visit).isNotNull();
            assertThat(visit.getRelativePath("/")).isEqualTo("README.md");
            assertThat(visit.getPath()).exists();
            assertThat(visit.getRoot()).isEqualTo(root);
            try {
                assertThat(Files.readString(visit.getPath())).isEqualTo("test readme");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void acceptNonExistentPath() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        tree.accept("non-existent", visit -> {
            assertThat(visit).isNull();
        });
    }

    @Test
    public void acceptUnixAbsolutePath() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        try {
            tree.accept("/README.md", visit -> {
                fail("Absolute paths aren't allowed");
            });
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void acceptOSSpecificAbsolutePath() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        try {
            tree.accept(root.resolve("README.md").toAbsolutePath().toString(), visit -> {
                fail("Absolute paths aren't allowed");
            });
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void acceptIllegalAbsolutePathOutsideTree() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        final Path absolute = root.getParent().resolve("external.txt");
        assertThat(absolute).exists();
        try {
            tree.accept(absolute.toString(), visit -> {
                fail("Absolute paths aren't allowed");
            });
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void acceptExistingRelativeNonNormalizedPath() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        assertThatThrownBy(() -> {
            tree.accept("other/../README.md", visit -> {
                fail("'..' should lead to an exception that prevents the visitor from being called");
            });
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'..' cannot be used in resource paths");
    }

    @Test
    public void acceptExistingRelativeNonNormalizedPathOutsideTree() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        assertThatThrownBy(() -> {
            tree.accept("../root/./other/../README.md", visit -> {
                fail("'..' should lead to an exception that prevents the visitor from being called");
            });
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'..' cannot be used in resource paths");
    }

    @Test
    public void acceptNonExistentRelativeNonNormalizedPathOutsideTree() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        assertThatThrownBy(() -> {
            tree.accept("../root/./README.md/../non-existent.txt", visit -> {
                fail("'..' should lead to an exception that prevents the visitor from being called");
            });
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'..' cannot be used in resource paths");
    }

    @Test
    public void walk() throws Exception {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        var visitor = new PathCollectingVisitor();
        tree.walk(visitor);
        assertThat(visitor.visitedPaths).containsExactlyInAnyOrderEntriesOf(getRawPaths());
    }

    @Test
    public void walkRaw() {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        var visitor = new PathCollectingVisitor();
        tree.walkRaw(visitor);
        assertThat(visitor.visitedPaths).containsExactlyInAnyOrderEntriesOf(getRawPaths());
    }

    @Test
    public void walkMultiReleaseEnabledDirectory() {
        final Path root = resolveTreeRoot("root");
        final PathTree tree = new DirectoryPathTree(root, null, true);
        var visitor = new PathCollectingVisitor();
        tree.walk(visitor);
        assertThat(visitor.visitedPaths).containsExactlyInAnyOrderEntriesOf(getMultiReleaseMappedPaths());
    }

    @Test
    public void isEmpty() throws Exception {
        final Path root = resolveTreeRoot("non-existing");
        assertThat(root).doesNotExist();
        final PathTree tree = new DirectoryPathTree(root);
        assertThat(tree.isEmpty()).isTrue();

        Path emptyDir = Files.createTempDirectory(baseDir, "empty");
        assertThat(emptyDir).exists();
        assertThat(tree.isEmpty()).isTrue();
    }

    /**
     * Returns a path relative to src/test/resources/paths/directory-path-tree/
     *
     * @param relative relative path
     * @return Path instance
     */
    private Path resolveTreeRoot(String relative) {
        return baseDir.resolve(relative);
    }
}
