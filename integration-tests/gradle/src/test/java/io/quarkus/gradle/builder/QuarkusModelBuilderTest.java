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
import io.quarkus.bootstrap.workspace.ProcessedSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;

class QuarkusModelBuilderTest {

    @Test
    public void shouldLoadSimpleModuleModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/simple-module-project");
        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(projectDir, "TEST");

        assertNotNull(quarkusModel);
        assertNotNull(quarkusModel.getApplicationModule());
        assertThat(quarkusModel.getWorkspaceModules()).isEmpty();
    }

    @Test
    public void shouldLoadMultiModuleModel() throws URISyntaxException, IOException {
        File projectDir = getResourcesProject("builder/multi-module-project");

        final ApplicationModel quarkusModel = QuarkusGradleModelFactory.create(new File(projectDir, "application"), "TEST");

        assertNotNull(quarkusModel);

        assertProjectModule(quarkusModel.getApplicationModule(),
                new File(projectDir, quarkusModel.getApplicationModule().getId().getArtifactId()), true);

        final Collection<WorkspaceModule> projectModules = quarkusModel.getWorkspaceModules();
        assertEquals(projectModules.size(), 1);
        for (WorkspaceModule p : projectModules) {
            assertProjectModule(p, new File(projectDir, p.getId().getArtifactId()), false);
        }
    }

    private void assertProjectModule(WorkspaceModule projectModule, File projectDir, boolean withTests) {
        assertNotNull(projectModule);
        assertEquals(projectDir, projectModule.getModuleDir());
        assertEquals(new File(projectDir, "build"), projectModule.getBuildDir());

        ProcessedSources src = projectModule.getMainSources().iterator().next();
        assertNotNull(src);
        assertThat(src.getDestinationDir()).isEqualTo(new File(projectDir, "build/classes/java/main"));
        assertThat(src.getSourceDir()).isEqualTo(new File(projectDir, "src/main/java"));

        src = projectModule.getMainResources().iterator().next();
        assertNotNull(src);
        assertThat(src.getDestinationDir()).isEqualTo(new File(projectDir, "build/resources/main"));
        assertThat(src.getSourceDir()).isEqualTo(new File(projectDir, "src/main/resources"));

        if (withTests) {
            src = projectModule.getTestSources().iterator().next();
            assertNotNull(src);
            assertThat(src.getDestinationDir()).isEqualTo(new File(projectDir, "build/classes/java/test"));
            assertThat(src.getSourceDir()).isEqualTo(new File(projectDir, "src/test/java"));

            src = projectModule.getTestResources().iterator().next();
            assertNotNull(src);
            assertThat(src.getDestinationDir()).isEqualTo(new File(projectDir, "build/resources/test"));
            assertThat(src.getSourceDir()).isEqualTo(new File(projectDir, "src/test/resources"));
        } else {
            assertThat(projectModule.getTestSources()).isEmpty();
            assertThat(projectModule.getTestResources()).isEmpty();
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
