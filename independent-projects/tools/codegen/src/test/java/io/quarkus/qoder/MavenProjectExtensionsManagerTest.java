package io.quarkus.qoder;

import io.quarkus.dependencies.Extension;
import io.quarkus.qoder.MavenProjectExtensionsManager.Range;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MavenProjectExtensionsManagerTest {

    private static final String VALID_POM_RESOURCE = "/valid-pom.xml";

    @Test
    void shouldDetectDependenciesRange() throws Exception {
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(getResourcePath(VALID_POM_RESOURCE));
        Assertions.assertThat(mgr.detectDependenciesRange()).hasValue(new Range(15, 35));
    }

    @Test
    void shouldAlsoConsiderTheGroupId() throws Exception {
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(getResourcePath(VALID_POM_RESOURCE));
        Assertions.assertThat(mgr.hasExtension(new Extension("org.other", "some-other-ext", "1.0.0"))).isTrue();
        Assertions.assertThat(mgr.hasExtension(new Extension("org.invalid", "some-other-ext", "1.0.0"))).isFalse();
    }

    @Test
    void shouldNotDetectCommentedDependency() throws Exception {
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(getResourcePath(VALID_POM_RESOURCE));
        Assertions.assertThat(mgr.hasExtension(new Extension("org.other", "some-commented-ext", "1.0.0"))).isFalse();
    }

    @Test
    void shouldDetectCommentedLines() throws Exception {
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(getResourcePath(VALID_POM_RESOURCE));
        Assertions.assertThat(mgr.detectCommentedLines()).containsExactlyInAnyOrder(16, 19, 23, 28, 29, 30, 31, 32, 33, 34);
    }

    @Test
    void shouldRemoveDependencyCorrectly() throws Exception {
        final Path dir = Files.createTempDirectory("proj");
        final Path pomPath = Files.copy(getResourcePath(VALID_POM_RESOURCE), dir.resolve("pom.xml"));
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(pomPath);
        final Extension extension = new Extension("io.quarkus", "some-ext", "*");
        Assertions.assertThat(mgr.hasExtension(extension)).isTrue();
        mgr.removeExtension(extension);
        Assertions.assertThat(mgr.hasExtension(extension)).isFalse();
    }

    private Path getResourcePath(String name) throws URISyntaxException {
        return Paths.get(MavenProjectExtensionsManagerTest.class.getResource(name).toURI());
    }

}
