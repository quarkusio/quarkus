package io.quarkus.it.container.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class JibBuildTarTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("jib-build-tar")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "quarkus")
            .overrideConfigKey("quarkus.container-image.build", "true")
            .overrideConfigKey("quarkus.container-image.tar.enabled", "true")
            // avoid pulling a base image, the test is only about the destination of the built image
            .overrideConfigKey("quarkus.jib.base-jvm-image", "scratch")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-container-image-jib", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testImageIsWrittenToTarball() throws IOException {
        Path imageTarFile = prodModeTestResults.getBuildDir().resolve("quarkus-jib-build-tar-0.1-SNAPSHOT.tar");
        assertThat(imageTarFile).isRegularFile();
        // the manifest.json entry of the tarball is not compressed
        assertThat(Files.readString(imageTarFile, StandardCharsets.ISO_8859_1))
                .contains("\"quarkus/jib-build-tar:0.1-SNAPSHOT\"");
        assertThat(prodModeTestResults.getBuildDir().resolve("jib-image.digest")).isRegularFile();
    }
}
