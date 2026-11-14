package io.quarkus.it.container.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class JibExtraDirectoriesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("jib-extra-directories")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "quarkus")
            .overrideConfigKey("quarkus.container-image.build", "true")
            .overrideConfigKey("quarkus.container-image.tar.enabled", "true")
            // avoid pulling a base image, the test is only about the files added to the image
            .overrideConfigKey("quarkus.jib.base-jvm-image", "scratch")
            // relative to the project directory
            .overrideConfigKey("quarkus.jib.extra-directories.greetings.from", "src/test/jib-extra-directory")
            .overrideConfigKey("quarkus.jib.extra-directories.greetings.into", "/opt/greetings")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-container-image-jib", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testExtraDirectoryIsAddedToImage() throws IOException {
        Path imageTarFile = prodModeTestResults.getBuildDir().resolve("quarkus-jib-extra-directories-0.1-SNAPSHOT.tar");
        assertThat(imageTarFile).isRegularFile();

        Map<String, String> layerFiles = new HashMap<>();
        try (InputStream image = Files.newInputStream(imageTarFile)) {
            readTar(image, (name, content) -> {
                if (name.endsWith(".tar.gz")) {
                    try (InputStream layer = new GZIPInputStream(new BufferedInputStream(content))) {
                        readTar(layer, (layerFileName, layerFileContent) -> layerFiles.put(layerFileName,
                                new String(layerFileContent.readAllBytes(), StandardCharsets.UTF_8)));
                    }
                }
            });
        }
        assertThat(layerFiles).containsEntry("opt/greetings/nested/greeting.txt", "Hello from an extra directory\n");
    }

    /**
     * Minimal reader of the regular files of a tar archive, enough for the archives written by Jib.
     */
    private static void readTar(InputStream tar, TarEntryConsumer consumer) throws IOException {
        byte[] header = new byte[512];
        while (tar.readNBytes(header, 0, header.length) == header.length && header[0] != 0) {
            String name = field(header, 0, 100);
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            long size = Long.parseLong(field(header, 124, 12).trim(), 8);
            byte[] content = tar.readNBytes((int) size);
            // the content is padded to a multiple of the block size
            tar.skipNBytes((512 - size % 512) % 512);
            if (header[156] == '0' || header[156] == 0) {
                consumer.accept(name, new ByteArrayInputStream(content));
            }
        }
    }

    private static String field(byte[] header, int offset, int length) {
        int end = offset;
        while (end < offset + length && header[end] != 0) {
            end++;
        }
        return new String(header, offset, end - offset, StandardCharsets.US_ASCII);
    }

    @FunctionalInterface
    private interface TarEntryConsumer {
        void accept(String name, InputStream content) throws IOException;
    }
}
