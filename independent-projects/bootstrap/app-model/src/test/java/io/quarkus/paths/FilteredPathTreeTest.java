package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.fs.util.ZipUtils;

public class FilteredPathTreeTest {

    @TempDir
    static Path testDir;
    static Path testJar;

    private static void createFile(String path) throws Exception {
        var file = testDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        createFile("META-INF/jandex.idx");
        createFile("org/toolbox/Axe.class");
        createFile("org/toolbox/Hammer.class");
        createFile("org/toolbox/Saw.class");
        createFile("README.md");
        testJar = testDir.resolve("test.jar");
        ZipUtils.zip(testDir, testJar);
    }

    @Test
    public void unfilteredTestDir() {
        var pathTree = PathTree.ofDirectoryOrArchive(testDir);
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "",
                "META-INF",
                "META-INF/jandex.idx",
                "org",
                "org/toolbox",
                "org/toolbox/Axe.class",
                "org/toolbox/Hammer.class",
                "org/toolbox/Saw.class",
                "README.md",
                "test.jar");
    }

    @Test
    public void unfilteredTestJar() {
        var pathTree = PathTree.ofDirectoryOrArchive(testJar);
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "",
                "META-INF",
                "META-INF/jandex.idx",
                "org",
                "org/toolbox",
                "org/toolbox/Axe.class",
                "org/toolbox/Hammer.class",
                "org/toolbox/Saw.class",
                "README.md",
                "test.jar");
    }

    @Test
    public void dirIncludeToolbox() {
        var pathTree = PathTree.ofDirectoryOrArchive(testDir, PathFilter.forIncludes(List.of("*/toolbox/**")));
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "org/toolbox/Axe.class",
                "org/toolbox/Hammer.class",
                "org/toolbox/Saw.class");
    }

    @Test
    public void jarIncludeToolbox() {
        var pathTree = PathTree.ofDirectoryOrArchive(testJar, PathFilter.forIncludes(List.of("*/toolbox/**")));
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "org/toolbox/Axe.class",
                "org/toolbox/Hammer.class",
                "org/toolbox/Saw.class");
    }

    @Test
    public void dirIncludeToolboxExcludeHammer() {
        var pathTree = PathTree.ofDirectoryOrArchive(testDir, new PathFilter(
                List.of("*/toolbox/**"),
                List.of("**/Hammer.class")));
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "org/toolbox/Axe.class",
                "org/toolbox/Saw.class");
    }

    @Test
    public void jarIncludeToolboxExcludeHammer() {
        var pathTree = PathTree.ofDirectoryOrArchive(testJar, new PathFilter(
                List.of("*/toolbox/**"),
                List.of("**/Hammer.class")));
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "org/toolbox/Axe.class",
                "org/toolbox/Saw.class");
    }

    @Test
    public void filteredPathTree() throws Exception {
        var originalFilter = new PathFilter(
                List.of("*/toolbox/**"),
                List.of("**/Hammer.class"));
        var outerFilter = new PathFilter(
                List.of("**/Axe.class"),
                List.of("**/Saw.class"));

        var pathTree = PathTree.ofDirectoryOrArchive(testDir, originalFilter);
        pathTree = new FilteredPathTree(pathTree, outerFilter);
        assertFilteredPathTree(pathTree);
        try (var openTree = pathTree.open()) {
            assertFilteredPathTree(openTree);
        }

        pathTree = PathTree.ofDirectoryOrArchive(testJar, originalFilter);
        pathTree = new FilteredPathTree(pathTree, outerFilter);
        assertFilteredPathTree(pathTree);
        try (var openTree = pathTree.open()) {
            assertFilteredPathTree(openTree);
        }
    }

    private static void assertFilteredPathTree(PathTree pathTree) {
        assertThat(getAllPaths(pathTree)).containsExactlyInAnyOrder(
                "org/toolbox/Axe.class");

        assertThat(pathTree.isEmpty()).isFalse();
        assertThat(pathTree.contains("org/toolbox/Axe.class")).isTrue();
        assertThat(pathTree.apply("org/toolbox/Axe.class", Objects::nonNull)).isTrue();
        assertThat(pathTree.apply("org/toolbox/Saw.class", Objects::nonNull)).isFalse();
        pathTree.accept("org/toolbox/Axe.class", visit -> assertThat(visit).isNotNull());
        pathTree.accept("org/toolbox/Saw.class", visit -> assertThat(visit).isNull());
    }

    private static Set<String> getAllPaths(PathTree pathTree) {
        final Set<String> paths = new HashSet<>();
        pathTree.walk(visit -> paths.add(visit.getRelativePath("/")));
        return paths;
    }
}
