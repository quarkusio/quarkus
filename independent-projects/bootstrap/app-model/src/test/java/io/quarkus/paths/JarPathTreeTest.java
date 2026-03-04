package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.fs.util.ZipUtils;

public class JarPathTreeTest {

    private static final String BASE_DIR = "paths/directory-path-tree";

    private static Path root;

    @BeforeAll
    public static void staticInit() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(BASE_DIR + "/root");
        if (url == null) {
            throw new IllegalStateException("Failed to locate " + BASE_DIR + " on the classpath");
        }
        final Path rootDir = Path.of(url.toURI()).toAbsolutePath();
        if (!Files.exists(rootDir)) {
            throw new IllegalStateException("Failed to locate " + rootDir);
        }

        root = rootDir.getParent().resolve("root.jar");
        ZipUtils.zip(rootDir, root);
    }

    @Test
    public void acceptExistingPath() throws Exception {
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
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        tree.accept("non-existent", visit -> {
            assertThat(visit).isNull();
        });
    }

    @Test
    public void acceptUnixAbsolutePath() throws Exception {
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
    public void acceptAbsolutePathOutsideTree() throws Exception {
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        final Path absolute = root.getParent().resolve("external.txt");
        assertThat(absolute).exists();
        try {
            tree.accept(absolute.toString(), visit -> {
                fail("Absolute paths aren't allowed");
            });
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void acceptExistingRelativeNonNormalizedPath() throws Exception {
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
        final PathTree tree = PathTree.ofDirectoryOrArchive(root);
        var visitor = new PathCollectingVisitor();
        tree.walk(visitor);
        assertThat(visitor.visitedPaths).containsExactlyInAnyOrderEntriesOf(DirectoryPathTreeTest.getMultiReleaseMappedPaths());
    }
}
