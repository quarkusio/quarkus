package io.quarkus.bootstrap.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ZipUtils}
 */
public class ZipUtilsTest {

    /**
     * Test that when an operation on a corrupt zip file fails, the resulting IOException
     * has the path details of the file/uri of the source file
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/4126"/>
     */
    @Test
    public void testCorruptZipException() throws Exception {
        final Path tmpFile = Files.createTempFile(null, ".jar");
        try {
            final URI uri = new URI("jar", tmpFile.toUri().toString(), null);
            try {
                ZipUtils.newFileSystem(uri, Collections.emptyMap());
                Assertions.fail("New filesystem creation was expected to fail for a non-zip file");
            } catch (IOException ioe) {
                // verify the exception message content
                if (!ioe.getMessage().contains(uri.toString())) {
                    throw ioe;
                }
            }

            try {
                ZipUtils.newFileSystem(tmpFile);
                Assertions.fail("New filesystem creation was expected to fail for a non-zip file");
            } catch (IOException ioe) {
                // verify the exception message content
                if (!ioe.getMessage().contains(tmpFile.toString())) {
                    throw ioe;
                }
            }
        } finally {
            Files.delete(tmpFile);
        }
    }

    /**
     * Test that the {@link ZipUtils#newZip(Path)} works as expected
     *
     * @throws Exception
     */
    @Test
    public void testNewZip() throws Exception {
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        final Path zipPath = Paths.get(tmpDir.toString(), "ziputilstest-" + System.currentTimeMillis() + ".jar");
        final Instant buildTime = Instant.parse("2006-12-03T10:15:25.35Z");
        final FileTime expectedTime = FileTime.from(Instant.parse("2006-12-03T10:15:25Z"));
        try {
            try (final FileSystem fs = ZipUtils.newZip(zipPath, buildTime)) {
                final Path someFileInZip = fs.getPath("hello.txt");
                Files.write(someFileInZip, "hello".getBytes(StandardCharsets.UTF_8));
            }
            // now just verify that the content was actually written out
            try (final FileSystem fs = ZipUtils.newFileSystem(zipPath)) {
                assertFileExistsWithContent(fs.getPath("hello.txt"), "hello", expectedTime);
            }
        } finally {
            Files.deleteIfExists(zipPath);
        }
    }

    /**
     * Tests that the {@link ZipUtils#newZip(Path)} works correctly, and creates the zip file,
     * when the parent directories of the {@link Path} passed to it are not present.
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/5680"/>
     */
    @Test
    public void testNewZipForNonExistentParentDir() throws Exception {
        final Path tmpDir = Files.createTempDirectory(null);
        final Path nonExistentLevel1Dir = tmpDir.resolve("non-existent-level1");
        final Path nonExistentLevel2Dir = nonExistentLevel1Dir.resolve("non-existent-level2");
        final Path zipPath = Paths.get(nonExistentLevel2Dir.toString(), "ziputilstest-nonexistentdirs.jar");
        final Instant buildTime = Instant.parse("2006-12-03T10:15:24.35Z");
        final FileTime expectedTime = FileTime.from(Instant.parse("2006-12-03T10:15:24Z"));
        try {
            try (final FileSystem fs = ZipUtils.newZip(zipPath, buildTime)) {
                final Path someFileInZip = fs.getPath("hello.txt");
                Files.write(someFileInZip, "hello".getBytes(StandardCharsets.UTF_8));
            }
            // now just verify that the content was actually written out
            try (final FileSystem fs = ZipUtils.newFileSystem(zipPath)) {
                assertFileExistsWithContent(fs.getPath("hello.txt"), "hello", expectedTime);
            }
        } finally {
            Files.deleteIfExists(zipPath);
        }
    }

    private static void assertFileExistsWithContent(final Path path, final String content, final FileTime expectedTime) throws IOException {
        final String readContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(content, readContent, "Unexpected content in " + path);
        final BasicFileAttributes attribs = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        assertEquals(expectedTime, attribs.creationTime());
        assertEquals(expectedTime, attribs.lastAccessTime());
        assertEquals(expectedTime, attribs.lastModifiedTime());
    }
}
