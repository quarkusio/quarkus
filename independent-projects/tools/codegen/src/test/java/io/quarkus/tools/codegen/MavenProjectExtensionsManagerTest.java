package io.quarkus.tools.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.dependencies.Extension;
import io.quarkus.tools.codegen.MavenProjectExtensionsManager.Range;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class MavenProjectExtensionsManagerTest {

    private static final String VALID_POM_RESOURCE = "/valid-pom.xml";

    @Test
    void shouldDetectDependenciesRange() throws Exception {
        final Path projectPath = prepareProjectDirWithPom(VALID_POM_RESOURCE);
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(projectPath);
        assertThat(mgr.detectDependenciesRange()).hasValue(new Range(15, 39));
    }

    @Test
    void shouldAlsoConsiderTheGroupId() throws Exception {
        final Path projectPath = prepareProjectDirWithPom(VALID_POM_RESOURCE);
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(projectPath);
        assertThat(mgr.hasExtension(new Extension("org.other", "some-other-ext", "1.0.0"))).isTrue();
        assertThat(mgr.hasExtension(new Extension("org.invalid", "some-other-ext", "1.0.0"))).isFalse();
        assertThat(mgr.hasExtension(new Extension("io.quarkus", "some-ext-2", "1.0.0"))).isTrue();
    }

    @Test
    void shouldNotDetectCommentedDependency() throws Exception {
        final Path projectPath = prepareProjectDirWithPom(VALID_POM_RESOURCE);
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(projectPath);
        assertThat(mgr.hasExtension(new Extension("org.other", "some-commented-ext", "1.0.0"))).isFalse();
    }

    @Test
    void shouldDetectCommentedLines() throws Exception {
        final Path projectPath = prepareProjectDirWithPom(VALID_POM_RESOURCE);
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(projectPath);
        assertThat(mgr.detectCommentedLines()).containsExactlyInAnyOrder(16, 19, 23, 28, 29, 30, 31, 32, 33, 34);
    }

    @Test
    void shouldRemoveDependencyCorrectly() throws Exception {
        final Path projectPath = prepareProjectDirWithPom(VALID_POM_RESOURCE);
        final MavenProjectExtensionsManager mgr = new MavenProjectExtensionsManager(projectPath);
        final Extension extension = new Extension("io.quarkus", "some-ext", "*");
        assertThat(mgr.hasExtension(extension)).isTrue();
        mgr.removeExtension(extension);
        assertThat(mgr.hasExtension(extension)).isFalse();
    }

    private Path prepareProjectDirWithPom(String resource) throws Exception {
        final Path dir = Files.createTempDirectory("proj");
        final Path pomPath = Files.copy(getResourcePath(VALID_POM_RESOURCE), dir.resolve("pom.xml"));
        return dir;
    }

    private Path getResourcePath(String name) throws URISyntaxException {
        return Paths.get(MavenProjectExtensionsManagerTest.class.getResource(name).toURI());
    }

}
