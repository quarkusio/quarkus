package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class QuarkusBuildItemDocTest {

    @TempDir
    Path tempDir;

    @Test
    public void gitRefDefaultsToMain() {
        QuarkusBuildItemDoc doc = new QuarkusBuildItemDoc();
        assertEquals("main", doc.gitRef);
    }

    @Test
    public void releaseVersionArgSetsGitRef() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("extensions"));
        Path outputFile = tempDir.resolve("output.adoc");

        QuarkusBuildItemDoc doc = parseArgs(outputFile.toString(), sourceDir.toString(), "3.21.0");

        assertEquals("3.21.0", doc.gitRef);
        assertEquals(List.of(sourceDir), doc.paths);
    }

    @Test
    public void snapshotVersionKeepsMainAsGitRef() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("extensions"));
        Path outputFile = tempDir.resolve("output.adoc");

        QuarkusBuildItemDoc doc = parseArgs(outputFile.toString(), sourceDir.toString(), "999-SNAPSHOT");

        assertEquals("main", doc.gitRef);
        assertEquals(List.of(sourceDir), doc.paths);
    }

    @Test
    public void noVersionArgKeepsMainAsGitRef() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("extensions"));
        Path outputFile = tempDir.resolve("output.adoc");

        QuarkusBuildItemDoc doc = parseArgs(outputFile.toString(), sourceDir.toString());

        assertEquals("main", doc.gitRef);
        assertEquals(List.of(sourceDir), doc.paths);
    }

    @Test
    public void multipleSourceDirsWithVersion() throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("core"));
        Path dir2 = Files.createDirectory(tempDir.resolve("extensions"));
        Path outputFile = tempDir.resolve("output.adoc");

        QuarkusBuildItemDoc doc = parseArgs(outputFile.toString(), dir1.toString(), dir2.toString(), "3.21.0");

        assertEquals("3.21.0", doc.gitRef);
        assertEquals(List.of(dir1, dir2), doc.paths);
    }

    private static QuarkusBuildItemDoc parseArgs(String... args) {
        QuarkusBuildItemDoc doc = new QuarkusBuildItemDoc();
        doc.outputFile = Path.of(args[0]);

        int sourceArgCount = args.length;
        String lastArg = args[args.length - 1];
        if (!Files.isDirectory(Path.of(lastArg))) {
            sourceArgCount--;
            if (!lastArg.endsWith("-SNAPSHOT")) {
                doc.gitRef = lastArg;
            }
        }
        doc.paths = Arrays.stream(args, 1, sourceArgCount)
                .map(Path::of)
                .collect(Collectors.toList());

        return doc;
    }
}
