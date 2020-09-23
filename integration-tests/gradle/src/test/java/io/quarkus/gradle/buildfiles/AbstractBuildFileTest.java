package io.quarkus.gradle.buildfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.buildfile.BuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

abstract class AbstractBuildFileTest {

    protected static <T extends BuildFile> T initializeProject(String projectDir,
            BiFunction<Path, QuarkusPlatformDescriptor, T> buildFileCreator)
            throws IOException, URISyntaxException {
        URL url = GroovyBuildFileTest.class.getClassLoader().getResource(projectDir);
        URI uri = url.toURI();
        Path gradleProjectPath = Paths.get(uri);

        final File projectProps = new File(gradleProjectPath.toFile(), "gradle.properties");
        if (!projectProps.exists()) {
            throw new IllegalStateException("Failed to locate " + projectProps);
        }
        final Properties props = new Properties();
        try (InputStream is = new FileInputStream(projectProps)) {
            props.load(is);
        }
        final String quarkusVersion = getQuarkusVersion();
        props.setProperty("quarkusPluginVersion", quarkusVersion);
        try (OutputStream os = new FileOutputStream(projectProps)) {
            props.store(os, "Quarkus Gradle TS");
        }

        final QuarkusPlatformDescriptor descriptor = Mockito.mock(QuarkusPlatformDescriptor.class);
        return buildFileCreator.apply(gradleProjectPath, descriptor);
    }

    protected static String getQuarkusVersion() throws IOException {
        final Path curDir = Paths.get("").toAbsolutePath().normalize();
        final Path gradlePropsFile = curDir.resolve("gradle.properties");
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(gradlePropsFile)) {
            props.load(is);
        }
        final String quarkusVersion = props.getProperty("version");
        if (quarkusVersion == null) {
            throw new IllegalStateException("Failed to locate Quarkus version in " + gradlePropsFile);
        }
        return quarkusVersion;
    }

    abstract String getProperty(String propertyName) throws IOException;

    abstract List<AppArtifactCoords> getDependencies() throws IOException;

    @Test
    void testGetDependencies() throws IOException {
        List<AppArtifactCoords> dependencies = getDependencies();
        assertThat(dependencies).isNotEmpty();
        List<String> depsString = new ArrayList<>();
        for (Iterator<AppArtifactCoords> depIter = dependencies.iterator(); depIter.hasNext();) {
            AppArtifactCoords dependency = depIter.next();
            String managementKey = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getType();
            if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                managementKey += ':' + dependency.getVersion();
            }
            depsString.add(managementKey);
        }
        assertThat(depsString).contains("io.quarkus:quarkus-jsonp:jar:0.23.2",
                "io.quarkus:quarkus-jsonb:jar:0.23.2", "io.quarkus:quarkus-resteasy:jar:0.23.2");
    }

    @Test
    void testGetProperty() throws IOException {
        assertNull(getProperty("toto"));
        assertEquals("0.23.2", getProperty("quarkusVersion"));
    }

    @Test
    void testGetTestClosure() throws IOException {
        assertNull(getProperty("test"));
    }

}
