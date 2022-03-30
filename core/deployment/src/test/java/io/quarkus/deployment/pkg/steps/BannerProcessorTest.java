package io.quarkus.deployment.pkg.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.steps.BannerProcessor;
import io.quarkus.fs.util.ZipUtils;

public class BannerProcessorTest {

    class MyBannerProcessor extends BannerProcessor {
        public boolean test(Path path) throws Exception {
            return this.isQuarkusCoreBanner(path.toUri().toURL());
        }
    }

    @Test
    public void checkQuarkusCoreBannerOnFilesystemWithSpecialCharacters() throws Exception {
        MyBannerProcessor processor = new MyBannerProcessor();

        assertFalse(processor.test(Paths.get("tmp", "Descărcări", "test", "something!")));

        final Path tmpDir = Files.createTempDirectory("Descărcări");
        final Path zipPath = tmpDir.resolve("BannerProcessorTest.jar");

        try {
            try (FileSystem ignored = ZipUtils.newZip(zipPath)) {
            }

            try (FileSystem fs = ZipUtils.newFileSystem(zipPath)) {
                assertFalse(processor.test(fs.getPath("/")));
            }

            try (final FileSystem fs = ZipUtils.newFileSystem(zipPath)) {
                Path classFile = fs.getPath(MyBannerProcessor.class.getName().replace('.', '/') + ".class");
                Files.createDirectories(classFile.getParent());
                Files.write(classFile, "".getBytes(StandardCharsets.UTF_8));
            }

            try (FileSystem fs = ZipUtils.newFileSystem(zipPath)) {
                assertTrue(processor.test(fs.getPath("/")));
            }
        } finally {
            Files.deleteIfExists(zipPath);
        }
    }
}
