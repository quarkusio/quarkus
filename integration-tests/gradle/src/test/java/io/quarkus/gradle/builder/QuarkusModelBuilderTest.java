package io.quarkus.gradle.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathTree;

class QuarkusModelBuilderTest {

    @Test
    public void shouldLoadSimpleModuleProdModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/simple-module-project");
        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(projectDir, "NORMAL");

        assertNotNull(quarkusModel);
        assertNotNull(quarkusModel.getApplicationModule());
        assertThat(quarkusModel.getWorkspaceModules().size()).isEqualTo(1);

        final ResolvedDependency appArtifact = quarkusModel.getAppArtifact();
        assertThat(appArtifact).isNotNull();
        assertThat(appArtifact.getWorkspaceModule()).isNotNull();
        final ArtifactSources testSources = appArtifact.getWorkspaceModule().getTestSources();
        assertThat(testSources).isNull();
    }

    @Test
    public void shouldLoadSimpleModuleTestModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/simple-module-project");
        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(projectDir, "TEST", "testClasses");

        assertNotNull(quarkusModel);
        assertNotNull(quarkusModel.getApplicationModule());
        assertThat(quarkusModel.getWorkspaceModules().size()).isEqualTo(1);

        final ResolvedDependency appArtifact = quarkusModel.getAppArtifact();
        assertThat(appArtifact).isNotNull();
        assertThat(appArtifact.getWorkspaceModule()).isNotNull();
        final ArtifactSources testSources = appArtifact.getWorkspaceModule().getTestSources();
        assertThat(testSources).isNotNull();
    }

    @Test
    public void shouldLoadSimpleModuleDevModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/simple-module-project");
        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(projectDir, "DEVELOPMENT", "testClasses");

        assertNotNull(quarkusModel);
        assertNotNull(quarkusModel.getApplicationModule());
        assertThat(quarkusModel.getWorkspaceModules().size()).isEqualTo(1);

        final ResolvedDependency appArtifact = quarkusModel.getAppArtifact();
        assertThat(appArtifact).isNotNull();
        assertThat(appArtifact.getWorkspaceModule()).isNotNull();
        final ArtifactSources testSources = appArtifact.getWorkspaceModule().getTestSources();
        assertThat(testSources).isNotNull();
    }

    @Test
    public void shouldLoadMultiModuleTestModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/multi-module-project");

        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(new File(projectDir, "application"), "TEST",
                "testClasses");

        assertNotNull(quarkusModel);

        assertProjectModule(quarkusModel.getApplicationModule(),
                new File(projectDir, quarkusModel.getApplicationModule().getId().getArtifactId()), true);

        final Collection<WorkspaceModule> projectModules = quarkusModel.getWorkspaceModules();
        assertEquals(2, projectModules.size());
        for (WorkspaceModule p : projectModules) {
            assertProjectModule(p, new File(projectDir, p.getId().getArtifactId()),
                    quarkusModel.getApplicationModule().getId().equals(p.getId()));
        }

        final ResolvedDependency appArtifact = quarkusModel.getAppArtifact();
        assertThat(appArtifact).isNotNull();
        assertThat(appArtifact.getWorkspaceModule()).isNotNull();
        final ArtifactSources testSources = appArtifact.getWorkspaceModule().getTestSources();
        assertThat(testSources).isNotNull();
        assertThat(testSources.getSourceDirs().size()).isEqualTo(1);
        final SourceDir testSrcDir = testSources.getSourceDirs().iterator().next();
        assertThat(testSrcDir.getDir())
                .isEqualTo(projectDir.toPath().resolve("application").resolve("src").resolve("test").resolve("java"));
        assertThat(testSources.getResourceDirs().size()).isEqualTo(1);
        final SourceDir testResourcesDir = testSources.getResourceDirs().iterator().next();
        assertThat(testResourcesDir.getDir())
                .isEqualTo(projectDir.toPath().resolve("application").resolve("src").resolve("test").resolve("resources"));
    }

    @Test
    public void shouldLoadMultiModuleDevModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/multi-module-project");

        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(new File(projectDir, "application"),
                "DEVELOPMENT");

        assertNotNull(quarkusModel);

        assertProjectModule(quarkusModel.getApplicationModule(),
                new File(projectDir, quarkusModel.getApplicationModule().getId().getArtifactId()), true);

        final Collection<WorkspaceModule> projectModules = quarkusModel.getWorkspaceModules();
        assertEquals(2, projectModules.size());
        for (WorkspaceModule p : projectModules) {
            assertProjectModule(p, new File(projectDir, p.getId().getArtifactId()),
                    quarkusModel.getApplicationModule().getId().equals(p.getId()));
        }

        final ResolvedDependency appArtifact = quarkusModel.getAppArtifact();
        assertThat(appArtifact).isNotNull();
        assertThat(appArtifact.getWorkspaceModule()).isNotNull();
        final ArtifactSources testSources = appArtifact.getWorkspaceModule().getTestSources();
        assertThat(testSources).isNotNull();
        assertThat(testSources.getSourceDirs().size()).isEqualTo(1);
        final SourceDir testSrcDir = testSources.getSourceDirs().iterator().next();
        assertThat(testSrcDir.getDir())
                .isEqualTo(projectDir.toPath().resolve("application").resolve("src").resolve("test").resolve("java"));
        assertThat(testSources.getResourceDirs().size()).isEqualTo(1);
        final SourceDir testResourcesDir = testSources.getResourceDirs().iterator().next();
        assertThat(testResourcesDir.getDir())
                .isEqualTo(projectDir.toPath().resolve("application").resolve("src").resolve("test").resolve("resources"));
    }

    private void assertProjectModule(WorkspaceModule projectModule, File projectDir, boolean withTests) {
        assertNotNull(projectModule);
        assertEquals(projectDir, projectModule.getModuleDir());
        assertEquals(new File(projectDir, "build"), projectModule.getBuildDir());

        SourceDir src = projectModule.getMainSources().getSourceDirs().iterator().next();
        assertNotNull(src);
        assertThat(src.getOutputDir()).isEqualTo(projectDir.toPath().resolve("build/classes/java/main"));
        PathTree sourceTree = src.getSourceTree();
        assertThat(sourceTree).isNotNull();
        assertThat(sourceTree.getRoots()).hasSize(1);
        assertThat(sourceTree.getRoots().iterator().next()).isEqualTo(projectDir.toPath().resolve("src/main/java"));

        src = projectModule.getMainSources().getResourceDirs().iterator().next();
        assertNotNull(src);
        assertThat(src.getOutputDir()).isEqualTo(projectDir.toPath().resolve("build/resources/main"));
        sourceTree = src.getSourceTree();
        assertThat(sourceTree).isNotNull();
        assertThat(sourceTree.getRoots()).hasSize(1);
        assertThat(sourceTree.getRoots().iterator().next()).isEqualTo(projectDir.toPath().resolve("src/main/resources"));

        if (withTests) {
            src = projectModule.getTestSources().getSourceDirs().iterator().next();
            assertNotNull(src);
            assertThat(src.getOutputDir()).isEqualTo(projectDir.toPath().resolve("build/classes/java/test"));
            sourceTree = src.getSourceTree();
            assertThat(sourceTree).isNotNull();
            assertThat(sourceTree.getRoots()).hasSize(1);
            assertThat(sourceTree.getRoots().iterator().next()).isEqualTo(projectDir.toPath().resolve("src/test/java"));

            src = projectModule.getTestSources().getResourceDirs().iterator().next();
            assertNotNull(src);
            assertThat(src.getOutputDir()).isEqualTo(projectDir.toPath().resolve("build/resources/test"));
            sourceTree = src.getSourceTree();
            assertThat(sourceTree).isNotNull();
            assertThat(sourceTree.getRoots()).hasSize(1);
            assertThat(sourceTree.getRoots().iterator().next()).isEqualTo(projectDir.toPath().resolve("src/test/resources"));
        } else {
            assertThat(projectModule.getTestSources().isOutputAvailable()).isFalse();
        }
    }

    private File getResourcesProject(String projectName) throws URISyntaxException, IOException {
        final URL basedirUrl = Thread.currentThread().getContextClassLoader().getResource(projectName);
        assertNotNull(basedirUrl);

        final File projectDir = new File(basedirUrl.toURI());

        final File projectProps = new File(projectDir, "gradle.properties");
        final Properties props = new Properties();
        final String quarkusVersion = getQuarkusVersion();
        props.setProperty("quarkusPlatformVersion", quarkusVersion);
        props.setProperty("quarkusPluginVersion", quarkusVersion);
        try (OutputStream os = new FileOutputStream(projectProps)) {
            props.store(os, "Quarkus Gradle TS");
        }
        return projectDir;
    }

    protected String getQuarkusVersion() throws IOException {
        final Path curDir = Paths.get("").toAbsolutePath().normalize();
        final Path gradlePropsFile = curDir.resolve("gradle.properties");
        Properties props = new Properties();
        try (InputStream is = java.nio.file.Files.newInputStream(gradlePropsFile)) {
            props.load(is);
        }
        final String quarkusVersion = props.getProperty("version");
        if (quarkusVersion == null) {
            throw new IllegalStateException("Failed to locate Quarkus version in " + gradlePropsFile);
        }
        return quarkusVersion;
    }
}
