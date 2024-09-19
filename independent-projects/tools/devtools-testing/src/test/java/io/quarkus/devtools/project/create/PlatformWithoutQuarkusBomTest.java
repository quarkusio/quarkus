package io.quarkus.devtools.project.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.paths.PathTree;

public class PlatformWithoutQuarkusBomTest extends MultiplePlatformBomsTestBase {

    private static final String MAIN_PLATFORM_KEY = "org.acme.platform";

    private static String prevMavenRepoLocalTail;

    @BeforeAll
    public static void setup() throws Exception {

        TestRegistryClientBuilder.newInstance()
                //.debug()
                .baseDir(configDir())
                // registry.acme.org
                .newRegistry("registry.acme.org")
                .newPlatform(MAIN_PLATFORM_KEY)
                .newStream("7.0")
                .newRelease("7.0.7")
                .quarkusVersion("2.2.2")
                .newMember("acme-magic-bom")
                .addExtension("acme-magic")
                .setPlatformProjectCodestartData("property-dump-codestart",
                        Map.of("property", Map.of("source", Map.of("acme-platform", "acme-platform"))))
                .release()
                .stream().platform()
                .registry()
                .clientBuilder()
                // Main Quarkus registry
                .newRegistry("registry.quarkus.org")
                // platform key
                .newPlatform("org.quarkus.platform")
                // 2.0 STREAM
                .newStream("2.0")
                // 2.0.4 release
                .newRelease("2.0.4")
                .quarkusVersion("2.2.2")
                // default bom including quarkus-core + essential metadata
                .addCoreMember()
                .addExtension("quarkus-magic")
                .addExtensionWithCodestart("quarkus-property-dump", "property-dump")
                .addCodestartsArtifact(ArtifactCoords.jar("org.acme", "acme-codestarts", "1.0"), packageCodestart())
                .setPlatformProjectCodestartData("property-dump-codestart",
                        Map.of("property", Map.of("source", Map.of("quarkus-platform", "quarkus-platform"))))
                .release()
                .newMember("quarkus-zoo-bom")
                .addExtension("quarkus-giraffe")
                .release()
                .registry()
                .clientBuilder()
                .build();

        prevMavenRepoLocalTail = System.setProperty("maven.repo.local.tail",
                TestRegistryClientBuilder.getMavenRepoDir(configDir()).toString());
        enableRegistryClient();
    }

    @AfterAll
    public static void afterAll() {
        if (prevMavenRepoLocalTail == null) {
            System.clearProperty("maven.repo.local.tail");
        } else {
            System.setProperty("maven.repo.local.tail", prevMavenRepoLocalTail);
        }
    }

    private static Path packageCodestart() {
        var url = Thread.currentThread().getContextClassLoader().getResource("codestarts");
        if (url == null) {
            throw new RuntimeException("Failed to locate codestarts directory on the classpath");
        }
        var dir = toLocalPath(url);
        if (!Files.isDirectory(dir)) {
            throw new RuntimeException(dir + " is not a directory");
        }

        var codestartsJar = Path.of("target").resolve("test-codestarts.jar");
        try {
            Files.deleteIfExists(codestartsJar);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (FileSystem fs = ZipUtils.newZip(codestartsJar)) {
            var codestartsDir = fs.getPath("codestarts");
            Files.createDirectories(codestartsDir);
            PathTree.ofDirectoryOrArchive(dir).walk(visit -> {
                final String relativePath = visit.getRelativePath();
                if (relativePath.isEmpty()) {
                    return;
                }
                try {
                    Files.copy(visit.getPath(), codestartsDir.resolve(visit.getRelativePath()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return codestartsJar;
    }

    private static Path toLocalPath(final URL url) {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to translate " + url + " to local path", e);
        }
    }

    protected String getMainPlatformKey() {
        return "org.quarkus.platform";
    }

    @Test
    public void testDeafultCodestart() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of());

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-resteasy-reactive", null)),
                "2.0.4");
    }

    @Test
    public void testQuarkusPlatformMagic() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("quarkus-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("org.quarkus.platform", "quarkus-magic", null)),
                "2.0.4");
    }

    @Test
    public void testAcmePlatformMagic() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("acme-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), ArtifactCoords.pom(MAIN_PLATFORM_KEY, "acme-magic-bom", "7.0.7")),
                List.of(ArtifactCoords.jar("org.acme.platform", "acme-magic", null)),
                "2.0.4");
    }

    @Test
    public void testMix() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("giraffe", "acme-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom(),
                        ArtifactCoords.pom("${quarkus.platform.group-id}", "quarkus-zoo-bom", "${quarkus.platform.version}"),
                        ArtifactCoords.pom(MAIN_PLATFORM_KEY, "acme-magic-bom", "7.0.7")),
                List.of(ArtifactCoords.jar("org.acme.platform", "acme-magic", null),
                        ArtifactCoords.jar("org.quarkus.platform", "quarkus-giraffe", null)),
                "2.0.4");
    }

    @Test
    public void testQuarkusPropertyDump() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("quarkus-property-dump"));

        assertModel(projectDir,
                List.of(mainPlatformBom()),
                List.of(ArtifactCoords.jar("org.quarkus.platform", "quarkus-property-dump", null)),
                "2.0.4");

        assertPropertyDump(projectDir, Map.of(
                "property.source.codestart", "codestart",
                "property.source.quarkus-platform", "quarkus-platform",
                "property.source.acme-platform", "codestart"));
    }

    @Test
    public void testAcmePropertyDump() throws Exception {
        final Path projectDir = newProjectDir("created-platform-wo-quarkus-bom");
        createProject(projectDir, List.of("quarkus-property-dump", "acme-magic"));

        assertModel(projectDir,
                List.of(mainPlatformBom(), ArtifactCoords.pom(MAIN_PLATFORM_KEY, "acme-magic-bom", "7.0.7")),
                List.of(ArtifactCoords.jar("org.quarkus.platform", "quarkus-property-dump", null),
                        ArtifactCoords.jar("org.acme.platform", "acme-magic", null)),
                "2.0.4");

        assertPropertyDump(projectDir, Map.of(
                "property.source.codestart", "codestart",
                "property.source.quarkus-platform", "quarkus-platform",
                "property.source.acme-platform", "acme-platform"));
    }

    private static void assertPropertyDump(Path projectDir, Map<String, String> expectedProps) throws IOException {
        Path propDump = projectDir.resolve("property-dump.txt");
        assertThat(propDump).exists();
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(propDump)) {
            props.load(reader);
        }
        assertThat(props).containsExactlyInAnyOrderEntriesOf(expectedProps);
    }
}
