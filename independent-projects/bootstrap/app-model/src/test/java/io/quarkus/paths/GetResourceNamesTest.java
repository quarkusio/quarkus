package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.fs.util.ZipUtils;

public class GetResourceNamesTest {

    @TempDir
    static Path testDir;
    static Path dir1;
    static Path dir2;
    static Path testJar;

    @BeforeAll
    public static void beforeAll() throws Exception {
        dir1 = testDir.resolve("dir1");
        createFile(dir1, "org/acme/Foo.class");
        createFile(dir1, "org/acme/Bar.class");

        dir2 = testDir.resolve("dir2");
        createFile(dir2, "org/acme/Baz.class");
        createFile(dir2, "META-INF/services/org.acme.Service");

        testJar = testDir.resolve("test.jar");
        ZipUtils.zip(dir1, testJar);
    }

    private static void createFile(Path root, String path) throws Exception {
        var file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }

    @Test
    public void emptyPathTree() {
        assertThat(EmptyPathTree.getInstance().getResourceNames()).isEmpty();
    }

    @Test
    public void filePathTree() {
        var file = dir1.resolve("org/acme/Foo.class");
        var tree = PathTree.ofDirectoryOrFile(file);
        assertThat(tree.getResourceNames()).containsExactly("Foo.class");
    }

    @Test
    public void multiRootPathTree() {
        var tree1 = PathTree.ofDirectoryOrArchive(dir1);
        var tree2 = PathTree.ofDirectoryOrArchive(dir2);
        var multi = new MultiRootPathTree(tree1, tree2);
        assertThat(multi.getResourceNames()).containsExactlyInAnyOrder(
                "",
                "org",
                "org/acme",
                "org/acme/Foo.class",
                "org/acme/Bar.class",
                "org/acme/Baz.class",
                "META-INF",
                "META-INF/services",
                "META-INF/services/org.acme.Service");
    }

    @Test
    public void multiRootPathTreeIsCached() {
        var tree1 = PathTree.ofDirectoryOrArchive(dir1);
        var tree2 = PathTree.ofDirectoryOrArchive(dir2);
        var multi = new MultiRootPathTree(tree1, tree2);
        var first = multi.getResourceNames();
        var second = multi.getResourceNames();
        assertThat(first).isSameAs(second);
    }

    @Test
    public void multiRootPathTreeEmpty() {
        var multi = new MultiRootPathTree();
        assertThat(multi.getResourceNames()).isEmpty();
    }

    @Test
    public void multiRootPathTreeDirAndJar() {
        var tree1 = PathTree.ofDirectoryOrArchive(dir2);
        var tree2 = PathTree.ofDirectoryOrArchive(testJar);
        var multi = new MultiRootPathTree(tree1, tree2);
        assertThat(multi.getResourceNames()).containsExactlyInAnyOrder(
                "",
                "org",
                "org/acme",
                "org/acme/Foo.class",
                "org/acme/Bar.class",
                "org/acme/Baz.class",
                "META-INF",
                "META-INF/services",
                "META-INF/services/org.acme.Service");
    }

    @Test
    public void getResourceNamesMatchesWalk() {
        var tree = PathTree.ofDirectoryOrArchive(dir1);
        var resourceNames = tree.getResourceNames();
        var walkNames = new java.util.HashSet<String>();
        tree.walk(visit -> walkNames.add(visit.getRelativePath()));
        assertThat(resourceNames).containsExactlyInAnyOrderElementsOf(walkNames);
    }
}
