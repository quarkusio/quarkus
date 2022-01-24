package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import io.quarkus.fs.util.ZipUtils;

public class CustomPackageConfigTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldContainsCustomManifestAttributes() throws Exception {
        File projectDir = getProjectDir("custom-packaging-property");

        runGradleWrapper(projectDir, "clean", "quarkusBuild");

        Path buildDir = new File(projectDir, "build").toPath();
        Path jar = buildDir.resolve("code-with-quarkus-1.0.0-SNAPSHOT-runner.jar");

        assertThat(jar).exists();
        try (FileSystem jarFs = ZipUtils.newFileSystem(jar)) {
            Path manifestPath = jarFs.getPath("META-INF/MANIFEST.MF");
            assertThat(manifestPath).exists();

            Manifest manifest = new Manifest();
            try (InputStream is = Files.newInputStream(manifestPath)) {
                manifest.read(is);
            }

            String customAttribute = manifest.getMainAttributes().getValue("Built-By");
            assertThat(customAttribute).isNotNull();
            assertThat(customAttribute).isEqualTo("Quarkus Plugin");
        }
    }

}
