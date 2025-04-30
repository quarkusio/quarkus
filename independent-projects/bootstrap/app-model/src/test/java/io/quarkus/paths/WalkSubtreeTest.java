package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.fs.util.ZipUtils;

public class WalkSubtreeTest {

    @TempDir
    static Path testDir;
    static Path testZip;

    private static void createFile(String path) throws Exception {
        var file = testDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }

    @BeforeAll
    public static void createFiles() throws Exception {
        createFile("a/1.txt");
        createFile("a/aa/1.txt");
        createFile("a/aa/aaa/1.txt");
        createFile("a/aa/aaa/2.txt");
        createFile("a/aa/aaa/aaaa/1.txt");

        createFile("b/aa/aaa/1.txt");
        createFile("b/aa/aaa/2.txt");
        createFile("b/aa/aaa/aaaa/1.txt");

        testZip = testDir.resolve("archive.zip");
        ZipUtils.zip(testDir, testZip);
    }

    @Test
    public void walkDirectorySubtree() {
        var list = new ArrayList<String>();
        PathTree.ofDirectoryOrArchive(testDir)
                .walkIfContains("a/aa", visit -> list.add(visit.getRelativePath()));
        assertThat(list).containsExactlyInAnyOrder(
                "a/aa",
                "a/aa/1.txt",
                "a/aa/aaa",
                "a/aa/aaa/1.txt",
                "a/aa/aaa/aaaa",
                "a/aa/aaa/aaaa/1.txt",
                "a/aa/aaa/2.txt");
    }

    @Test
    public void walkJarSubtree() {
        var list = new ArrayList<String>();
        PathTree.ofDirectoryOrArchive(testZip)
                .walkIfContains("a/aa", visit -> list.add(visit.getRelativePath()));
        assertThat(list).containsExactlyInAnyOrder(
                "a/aa",
                "a/aa/1.txt",
                "a/aa/aaa",
                "a/aa/aaa/1.txt",
                "a/aa/aaa/aaaa",
                "a/aa/aaa/aaaa/1.txt",
                "a/aa/aaa/2.txt");
    }

    @Test
    public void walkMultirootTree() {
        var list = new ArrayList<String>();
        new MultiRootPathTree(
                PathTree.ofDirectoryOrArchive(testDir.resolve("a/aa")),
                PathTree.ofDirectoryOrArchive(testDir.resolve("b/aa")))
                .walk(visit -> list.add(ensureForwardSlash(testDir.relativize(visit.getPath()).toString())));
        assertThat(list).containsExactlyInAnyOrder(
                "a/aa",
                "a/aa/1.txt",
                "a/aa/aaa",
                "a/aa/aaa/1.txt",
                "a/aa/aaa/aaaa",
                "a/aa/aaa/aaaa/1.txt",
                "a/aa/aaa/2.txt",
                "b/aa",
                "b/aa/aaa",
                "b/aa/aaa/1.txt",
                "b/aa/aaa/aaaa",
                "b/aa/aaa/aaaa/1.txt",
                "b/aa/aaa/2.txt");
    }

    @Test
    public void walkMultirootSubtree() {
        var list = new ArrayList<String>();
        new MultiRootPathTree(
                PathTree.ofDirectoryOrArchive(testDir.resolve("a/aa")),
                PathTree.ofDirectoryOrArchive(testDir.resolve("b/aa")))
                .walkIfContains("aaa", visit -> list.add(ensureForwardSlash(testDir.relativize(visit.getPath()).toString())));
        assertThat(list).containsExactlyInAnyOrder(
                "a/aa/aaa",
                "a/aa/aaa/1.txt",
                "a/aa/aaa/2.txt",
                "a/aa/aaa/aaaa",
                "a/aa/aaa/aaaa/1.txt",
                "b/aa/aaa",
                "b/aa/aaa/1.txt",
                "b/aa/aaa/2.txt",
                "b/aa/aaa/aaaa",
                "b/aa/aaa/aaaa/1.txt");
    }

    private static String ensureForwardSlash(String path) {
        return File.separatorChar == '/' ? path : path.replace(File.separatorChar, '/');
    }
}
