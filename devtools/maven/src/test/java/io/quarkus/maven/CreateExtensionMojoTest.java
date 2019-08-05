package io.quarkus.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class CreateExtensionMojoTest {

    static CreateExtensionMojo copyTestTree(String testProjectName, String testName) throws IllegalArgumentException,
            IllegalAccessException, IOException, NoSuchFieldException, SecurityException {
        final Path srcDir = Paths.get("src/test/resources/projects/" + testProjectName);
        /*
         * We want to run on the same project multiple times with different args so let's create a copy with a random
         * suffix
         */
        final Path copyDir = Paths
                .get("target/test-classes/projects/" + testProjectName + "-" + testName);
        Files.walk(srcDir).forEach(source -> {
            try {
                final Path dest = copyDir.resolve(srcDir.relativize(source));
                Files.copy(source, dest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return defaultMojo(copyDir);
    }

    public static CreateExtensionMojo defaultMojo(Path basedir) {
        final CreateExtensionMojo mojo = new CreateExtensionMojo();
        mojo.basedir = basedir;
        mojo.encoding = CreateExtensionMojo.DEFAULT_ENCODING;
        mojo.templatesUriBase = CreateExtensionMojo.DEFAULT_TEMPLATES_URI_BASE;
        mojo.quarkusVersion = CreateExtensionMojo.DEFAULT_QUARKUS_VERSION;
        mojo.assumeManaged = true;
        mojo.nameSegmentDelimiter = CreateExtensionMojo.DEFAULT_NAME_SEGMENT_DELIMITER;
        return mojo;
    }

    @Test
    void singleExtensionFromScratch() throws IOException, MojoExecutionException, MojoFailureException {
        final Path basedir = Paths
                .get("target/test-classes/projects/single-from-scratch");
        Files.createDirectories(basedir);
        final CreateExtensionMojo mojo = defaultMojo(basedir);
        mojo.groupId = "io.quarkus.example";
        mojo.artifactId = "single-ext";
        mojo.execute();

        assertTreesMatch(Paths.get("target/test-classes/expected/single-from-scratch"),
                mojo.basedir);
    }

    @Test
    void createExtensionUnderExistingPomMinimal() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = copyTestTree("create-extension-pom", "minimal");
        mojo.artifactId = "my-project-(minimal-extension)";
        mojo.assumeManaged = false;
        mojo.execute();

        assertTreesMatch(Paths.get("target/test-classes/expected/create-extension-pom-minimal"),
                mojo.basedir);
    }

    @Test
    void createExtensionUnderExistingPomCustomGrandParent() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = copyTestTree("create-extension-pom", "grand-parent");
        mojo.artifactId = "myproject-(with-grand-parent)";
        mojo.grandParentArtifactId = "build-bom";
        mojo.grandParentRelativePath = "../../build-bom/pom.xml";
        mojo.templatesUriBase = "file:templates";

        mojo.runtimeBomPath = Paths.get("boms/runtime/pom.xml");
        mojo.deploymentBomPath = Paths.get("boms/deployment/pom.xml");
        mojo.execute();

        assertTreesMatch(
                Paths.get("target/test-classes/expected/create-extension-pom-with-grand-parent"),
                mojo.basedir);
    }

    static void assertTreesMatch(Path expected, Path actual) throws IOException {
        final Set<Path> expectedFiles = new LinkedHashSet<>();
        Files.walk(expected).filter(Files::isRegularFile).forEach(p -> {
            final Path relative = expected.relativize(p);
            expectedFiles.add(relative);
            final Path actualPath = actual.resolve(relative);
            try {
                Assert.assertEquals(new String(Files.readAllBytes(p), StandardCharsets.UTF_8),
                        new String(Files.readAllBytes(actualPath), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        final Set<Path> unexpectedFiles = new LinkedHashSet<>();
        Files.walk(actual).filter(Files::isRegularFile).forEach(p -> {
            final Path relative = actual.relativize(p);
            if (!expectedFiles.contains(relative)) {
                unexpectedFiles.add(relative);
            }
        });
        if (!unexpectedFiles.isEmpty()) {
            Assert.fail(String.format("Files found under [%s] but not defined as expected under [%s]:%s", actual,
                    expected, unexpectedFiles.stream().map(Path::toString).collect(Collectors.joining("\n    "))));
        }
    }

    @Test
    void getPackage() throws IOException {
        Assert.assertEquals("org.apache.camel.quarkus.aws.sns.deployment", CreateExtensionMojo
                .getJavaPackage("org.apache.camel.quarkus", null, "camel-quarkus-aws-sns-deployment"));
        Assert.assertEquals("org.apache.camel.quarkus.component.aws.sns.deployment", CreateExtensionMojo
                .getJavaPackage("org.apache.camel.quarkus", "component", "camel-quarkus-aws-sns-deployment"));
    }

    @Test
    void toCapCamelCase() throws IOException {
        Assert.assertEquals("FooBarBaz", CreateExtensionMojo.toCapCamelCase("foo-bar-baz"));
    }

}
