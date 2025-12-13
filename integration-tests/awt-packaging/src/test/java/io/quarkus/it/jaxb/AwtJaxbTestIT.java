package io.quarkus.it.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.smallrye.common.os.OS;

@QuarkusIntegrationTest
public class AwtJaxbTestIT extends AwtJaxbTest {

    /**
     * Test is native image only, we need all artifacts to be packaged already, e.g. function.zip
     * </br>
     * Tests that the same set of dynamic lib files that was copied over from the remote build container is also packaged into
     * the zip file that will be deployed to AWS Lambda.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testPackaging() throws IOException {
        final String dynLibSuffix = switch (OS.current()) {
            case WINDOWS -> ".dll";
            case MAC -> ".dylib";
            default -> ".so";
        };
        final Path targetPath = Paths.get(".", "target").toAbsolutePath();
        final Set<String> localLibs = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath, "*" + dynLibSuffix)) {
            for (Path entry : stream) {
                localLibs.add(entry.getFileName().toString());
            }
        }
        assertFalse(localLibs.isEmpty(), "We built an AWT enabled app, there must be some libs next to our executable.");
        final Path zipPath = targetPath.resolve("function.zip").toAbsolutePath();
        assertTrue(Files.exists(zipPath), "Expected " + zipPath + " to exist");
        final Set<String> awsLambdaLibs = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(dynLibSuffix)) {
                    awsLambdaLibs.add(entry.getName());
                }
                zipInputStream.closeEntry();
            }
        }
        assertEquals(localLibs, awsLambdaLibs,
                "The sets of dynamic libs produced by the build and the set in .zip file MUST be the same. " +
                        "It was: " + localLibs + " vs. " + awsLambdaLibs);
    }
}
