package io.quarkus.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.utilities.MojoUtils;

public class CreateExtensionMojoTest {

    private static CreateExtensionMojo initMojo(final Path projectDir) throws IOException {
        final CreateExtensionMojo mojo = new CreateExtensionMojo();
        mojo.project = new MavenProject();
        mojo.basedir = projectDir.toFile();

        final File pom = new File(projectDir.toFile(), "pom.xml");
        if (pom.exists()) {
            mojo.project.setFile(pom);
            final Model rawModel = MojoUtils.readPom(pom);
            // the project would have an interpolated model at runtime, which we can't fully init here
            // here are just some key parts
            if (rawModel.getDependencyManagement() != null) {
                List<Dependency> deps = rawModel.getDependencyManagement().getDependencies();
                if (deps != null && !deps.isEmpty()) {
                    Dependency deploymentBom = null;
                    for (Dependency dep : deps) {
                        if (dep.getArtifactId().equals("quarkus-bom-deployment") && dep.getGroupId().equals("io.quarkus")) {
                            deploymentBom = dep;
                        }
                    }
                    if (deploymentBom != null) {
                        String version = deploymentBom.getVersion();
                        if (CreateExtensionMojo.QUARKUS_VERSION_POM_EXPR.equals(version)) {
                            version = rawModel.getProperties().getProperty(version.substring(2, version.length() - 1));
                            if (version == null) {
                                throw new IllegalStateException(
                                        "Failed to resolve " + deploymentBom.getVersion() + " from " + pom);
                            }
                        }
                        Dependency dep = new Dependency();
                        dep.setGroupId("io.quarkus");
                        dep.setArtifactId("quarkus-core-deployment");
                        dep.setType("jar");
                        dep.setVersion(version);
                        deps.add(dep);
                    }
                }
            }
            mojo.project.setModel(rawModel);
        }

        Build build = mojo.project.getBuild();
        if (build.getPluginManagement() == null) {
            build.setPluginManagement(new PluginManagement());
        }

        mojo.encoding = CreateExtensionMojo.DEFAULT_ENCODING;
        mojo.templatesUriBase = CreateExtensionMojo.DEFAULT_TEMPLATES_URI_BASE;
        mojo.quarkusVersion = CreateExtensionMojo.DEFAULT_QUARKUS_VERSION;
        mojo.bomEntryVersion = CreateExtensionMojo.DEFAULT_BOM_ENTRY_VERSION;
        mojo.assumeManaged = true;
        mojo.nameSegmentDelimiter = CreateExtensionMojo.DEFAULT_NAME_SEGMENT_DELIMITER;
        mojo.platformGroupId = CreateExtensionMojo.PLATFORM_DEFAULT_GROUP_ID;
        mojo.platformArtifactId = CreateExtensionMojo.PLATFORM_DEFAULT_ARTIFACT_ID;
        mojo.compilerPluginVersion = CreateExtensionMojo.COMPILER_PLUGIN_DEFAULT_VERSION;
        return mojo;
    }

    private static Path createProjectFromTemplate(String testProjectName) throws IOException {
        final Path srcDir = Paths.get("src/test/resources/projects/" + testProjectName);
        /*
         * We want to run on the same project multiple times with different args so let's create a copy with a random
         * suffix
         */
        final Path copyDir = newProjectDir(testProjectName);
        Files.walk(srcDir).forEach(source -> {
            final Path dest = copyDir.resolve(srcDir.relativize(source));
            try {
                Files.copy(source, dest);
            } catch (IOException e) {
                if (!Files.isDirectory(dest)) {
                    throw new RuntimeException(e);
                }
            }
        });
        return copyDir;
    }

    private static Path newProjectDir(String testProjectName) throws IOException {
        int count = 0;
        while (count < 100) {
            Path path = Paths.get("target/test-classes/projects/" + testProjectName + "-" + UUID.randomUUID());
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                return path;
            }
            count++;
        }

        // if we have tried too many times we just give up instead of looping forever which could cause the test to never end
        throw new RuntimeException("Unable to create a directory for copying the test application into");
    }

    @Test
    void createExtensionUnderExistingPomMinimal() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = initMojo(createProjectFromTemplate("create-extension-pom"));
        mojo.artifactId = "my-project-(minimal-extension)";
        mojo.assumeManaged = false;
        mojo.execute();

        assertTreesMatch(Paths.get("src/test/resources/expected/create-extension-pom-minimal"),
                mojo.basedir.toPath());
    }

    @Test
    void createExtensionUnderExistingPomWithAdditionalRuntimeDependencies() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = initMojo(createProjectFromTemplate("create-extension-pom"));
        mojo.artifactId = "my-project-(add-to-bom)";
        mojo.assumeManaged = false;
        mojo.runtimeBomPath = Paths.get("boms/runtime/pom.xml");
        mojo.additionalRuntimeDependencies = Arrays.asList("org.example:example-1:1.2.3",
                "org.acme:acme-@{quarkus.artifactIdBase}:@{$}{acme.version}");
        mojo.execute();

        assertTreesMatch(Paths.get("src/test/resources/expected/create-extension-pom-add-to-bom"),
                mojo.basedir.toPath());
    }

    @Test
    void createExtensionUnderExistingPomWithItest() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = initMojo(createProjectFromTemplate("create-extension-pom"));
        mojo.artifactId = "my-project-(itest)";
        mojo.assumeManaged = false;
        mojo.itestParentPath = Paths.get("integration-tests/pom.xml");
        mojo.execute();

        assertTreesMatch(Paths.get("src/test/resources/expected/create-extension-pom-itest"),
                mojo.basedir.toPath());
    }

    @Test
    void createExtensionUnderExistingPomCustomGrandParent() throws MojoExecutionException, MojoFailureException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
        final CreateExtensionMojo mojo = initMojo(createProjectFromTemplate("create-extension-pom"));
        mojo.artifactId = "myproject-(with-grand-parent)";
        mojo.grandParentArtifactId = "grand-parent";
        mojo.grandParentRelativePath = "../pom.xml";
        mojo.templatesUriBase = "file:templates";

        mojo.runtimeBomPath = Paths.get("boms/runtime/pom.xml");
        mojo.deploymentBomPath = Paths.get("boms/deployment/pom.xml");
        mojo.execute();
        assertTreesMatch(
                Paths.get("src/test/resources/expected/create-extension-pom-with-grand-parent"),
                mojo.basedir.toPath());
    }

    @Test
    void createNewExtensionProject() throws Exception {
        final CreateExtensionMojo mojo = initMojo(newProjectDir("new-ext-project"));
        mojo.groupId = "org.acme";
        mojo.artifactId = "my-ext";
        mojo.version = "1.0-SNAPSHOT";
        mojo.assumeManaged = null;
        mojo.execute();
        assertTreesMatch(
                Paths.get("target/test-classes/expected/new-extension-project"),
                mojo.basedir.toPath());
    }

    static void assertTreesMatch(Path expected, Path actual) throws IOException {
        final Set<Path> expectedFiles = new LinkedHashSet<>();
        Files.walk(expected).filter(Files::isRegularFile).forEach(p -> {
            final Path relative = expected.relativize(p);
            expectedFiles.add(relative);
            final Path actualPath = actual.resolve(relative);
            try {
                assertEquals(new String(Files.readAllBytes(p), StandardCharsets.UTF_8),
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
            fail(String.format("Files found under [%s] but not defined as expected under [%s]:%s", actual,
                    expected, unexpectedFiles.stream().map(Path::toString).collect(Collectors.joining("\n    "))));
        }
    }

    @Test
    void getPackage() throws IOException {
        assertEquals("org.apache.camel.quarkus.aws.sns.deployment", CreateExtensionMojo
                .getJavaPackage("org.apache.camel.quarkus", null, "camel-quarkus-aws-sns-deployment"));
        assertEquals("org.apache.camel.quarkus.component.aws.sns.deployment", CreateExtensionMojo
                .getJavaPackage("org.apache.camel.quarkus", "component", "camel-quarkus-aws-sns-deployment"));
    }

    @Test
    void toCapCamelCase() throws IOException {
        assertEquals("FooBarBaz", CreateExtensionMojo.toCapCamelCase("foo-bar-baz"));
    }

}
