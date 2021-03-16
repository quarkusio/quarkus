package io.quarkus.gradle.builder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.assertj.core.util.Files;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.SourceSet;
import io.quarkus.bootstrap.resolver.model.Workspace;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;

class QuarkusModelBuilderTest {

    @Test
    void testToAppDependency() {
        ResolvedArtifact artifact = mock(ResolvedArtifact.class);
        ResolvedModuleVersion version = mock(ResolvedModuleVersion.class);
        when(version.toString()).thenReturn(":commons-lang3-3.9:");
        when(artifact.getModuleVersion()).thenReturn(version);
        when(artifact.getFile()).thenReturn(Files.currentFolder());
        assertThatCode(() -> QuarkusModelBuilder.toDependency(artifact)).doesNotThrowAnyException();
    }

    @Test
    public void shouldLoadSimpleModuleModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/simple-module-project");
        final QuarkusModel quarkusModel = QuarkusGradleModelFactory.create(projectDir, "TEST");

        assertNotNull(quarkusModel);
        Workspace workspace = quarkusModel.getWorkspace();
        assertWorkspace(workspace.getMainModule(), projectDir);
        assertEquals(1, quarkusModel.getWorkspace().getAllModules().size());
    }

    @Test
    public void shouldLoadMultiModuleModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/multi-module-project");
        final QuarkusModel quarkusModel = QuarkusGradleModelFactory.create(new File(projectDir, "application"), "TEST");

        assertNotNull(quarkusModel);
        assertEquals(2, quarkusModel.getWorkspace().getAllModules().size());

        for (WorkspaceModule module : quarkusModel.getWorkspace().getAllModules()) {
            assertWorkspace(module, new File(projectDir, module.getArtifactCoords().getArtifactId()));
        }
    }

    private void assertWorkspace(WorkspaceModule workspaceModule, File projectDir) {
        assertNotNull(workspaceModule);
        assertEquals(projectDir, workspaceModule.getProjectRoot());
        assertEquals(new File(projectDir, "build"), workspaceModule.getBuildDir());
        final SourceSet sourceSet = workspaceModule.getSourceSet();
        assertNotNull(sourceSet);
        assertNull(sourceSet.getResourceDirectory());
        assertEquals(1, sourceSet.getSourceDirectories().size());
        assertEquals(new File(projectDir, "build/classes/java/main"), sourceSet.getSourceDirectories().iterator().next());
        final SourceSet sourceSourceSet = workspaceModule.getSourceSourceSet();
        assertEquals(new File(projectDir, "src/main/resources"), sourceSourceSet.getResourceDirectory());
        assertEquals(1, sourceSourceSet.getSourceDirectories().size());
        assertEquals(new File(projectDir, "src/main/java"), sourceSourceSet.getSourceDirectories().iterator().next());
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
