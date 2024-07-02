package io.quarkus.it.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@QuarkusIntegrationTest
public class AwtJaxbTestIT extends AwtJaxbTest {

    /**
     * Test is native image only, we need all artifacts to be packaged
     * already, e.g. function.zip
     * </br>
     * Tests that the same set of .so files that was copied over
     * from the remote build container is also packaged into the
     * zip file that will be deployed to AWS Lambda.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testPackaging() throws IOException {
        final Path targetPath = Paths.get(".", "target").toAbsolutePath();
        final Set<String> localLibs = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath, "*.so")) {
            for (Path entry : stream) {
                localLibs.add(entry.getFileName().toString());
            }
        }

        final Path zipPath = targetPath.resolve("function.zip").toAbsolutePath();
        assertTrue(Files.exists(zipPath), "Expected " + zipPath + " to exist");
        final Set<String> awsLambdaLibs = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".so")) {
                    awsLambdaLibs.add(entry.getName());
                }
                zipInputStream.closeEntry();
            }
        }
        assertEquals(localLibs, awsLambdaLibs,
                "The sets of .so libs produced by the build and the set in .zip file MUST be the same. It was: "
                        + localLibs + " vs. " + awsLambdaLibs);
    }
}
