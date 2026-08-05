package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MultiRootPathTreeTest {

    @TempDir
    static Path testDir;
    static Path dir1;
    static Path dir2;

    @BeforeAll
    public static void beforeAll() throws Exception {
        dir1 = testDir.resolve("dir1");
        createFile(dir1, "org/acme/Foo.class");
        createFile(dir1, "org/acme/Bar.class");
        createFile(dir1, "shared.txt");

        dir2 = testDir.resolve("dir2");
        createFile(dir2, "org/acme/Baz.class");
        createFile(dir2, "shared.txt");
    }

    private static void createFile(Path root, String path) throws Exception {
        var file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, path);
    }

    private static MultiRootPathTree getTestMultiRootTree() {
        return new MultiRootPathTree(
                PathTree.ofDirectoryOrArchive(dir1),
                PathTree.ofDirectoryOrArchive(dir2));
    }

    @Test
    public void acceptFindsResourceInFirstTree() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<String>();
        tree.accept("org/acme/Foo.class", visit -> {
            assertThat(visit).isNotNull();
            visited.add(visit.getResourceName());
        });
        assertThat(visited).containsExactly("org/acme/Foo.class");
    }

    @Test
    public void acceptFindsResourceInSecondTree() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<String>();
        tree.accept("org/acme/Baz.class", visit -> {
            assertThat(visit).isNotNull();
            visited.add(visit.getResourceName());
        });
        assertThat(visited).containsExactly("org/acme/Baz.class");
    }

    @Test
    public void acceptStopsAtFirstMatch() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<Path>();
        tree.accept("shared.txt", visit -> {
            assertThat(visit).isNotNull();
            visited.add(visit.getPath());
        });
        assertThat(visited).hasSize(1);
        assertThat(visited.get(0)).startsWith(dir1);
    }

    @Test
    public void acceptCallsConsumerWithNullWhenNotFound() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<PathVisit>();
        tree.accept("does-not-exist.txt", visited::add);
        assertThat(visited).containsExactly((PathVisit) null);
    }

    @Test
    public void acceptOnEmptyTree() {
        var tree = new MultiRootPathTree();
        var visited = new ArrayList<PathVisit>();
        tree.accept("anything.txt", visited::add);
        assertThat(visited).containsExactly((PathVisit) null);
    }

    @Test
    public void acceptAllVisitsAllMatches() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<Path>();
        tree.acceptAll("shared.txt", visit -> visited.add(visit.getPath()));
        assertThat(visited).hasSize(2);
        assertThat(visited.get(0)).startsWith(dir1);
        assertThat(visited.get(1)).startsWith(dir2);
    }

    @Test
    public void acceptAllCallsConsumerWithNullWhenNotFound() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<PathVisit>();
        tree.acceptAll("does-not-exist.txt", visited::add);
        assertThat(visited).containsExactly((PathVisit) null);
    }

    @Test
    public void acceptAllOnEmptyTree() {
        var tree = new MultiRootPathTree();
        var visited = new ArrayList<PathVisit>();
        tree.acceptAll("anything.txt", visited::add);
        assertThat(visited).containsExactly((PathVisit) null);
    }

    @Test
    public void acceptAllWithSingleMatch() {
        var tree = getTestMultiRootTree();
        var visited = new ArrayList<String>();
        tree.acceptAll("org/acme/Foo.class", visit -> visited.add(visit.getResourceName()));
        assertThat(visited).containsExactly("org/acme/Foo.class");
    }

    @Test
    public void applyReturnsResultFromFirstTree() {
        var tree = getTestMultiRootTree();
        var result = tree.apply("org/acme/Foo.class", PathVisit::getResourceName);
        assertThat(result).isEqualTo("org/acme/Foo.class");
    }

    @Test
    public void applyReturnsResultFromSecondTree() {
        var tree = getTestMultiRootTree();
        var result = tree.apply("org/acme/Baz.class", PathVisit::getResourceName);
        assertThat(result).isEqualTo("org/acme/Baz.class");
    }

    @Test
    public void applyReturnsFirstNonNullResult() {
        var tree = getTestMultiRootTree();
        var result = tree.apply("shared.txt", visit -> visit.getPath().toString());
        assertThat(result).startsWith(dir1.toString());
    }

    @Test
    public void applyReturnsNullWhenNotFound() {
        var tree = getTestMultiRootTree();
        var result = tree.apply("does-not-exist.txt", visit -> visit == null ? null : "non-null");
        assertThat(result).isNull();
    }

    @Test
    public void applyOnEmptyTree() {
        var tree = new MultiRootPathTree();
        var result = tree.apply("anything.txt", visit -> visit == null ? null : "non-null");
        assertThat(result).isNull();
    }

    @Test
    public void applyReturnsNonNullResultWhenNotFound() {
        var tree = getTestMultiRootTree();
        var result = tree.apply("does-not-exist.txt", visit -> {
            if (visit == null) {
                return true;
            }
            return false;
        });
        assertThat(result).isTrue();
    }
}
