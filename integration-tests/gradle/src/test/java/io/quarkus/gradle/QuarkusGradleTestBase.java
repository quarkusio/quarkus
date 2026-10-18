package io.quarkus.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import io.quarkus.bootstrap.util.IoUtils;

public class QuarkusGradleTestBase {

    protected File getProjectDir(final String projectName) {
        return getProjectDir(projectName, null);
    }

    protected File getProjectDir(final String baseProjectName, String testSuffix) {
        final URL projectUrl = Thread.currentThread().getContextClassLoader().getResource(baseProjectName);
        if (projectUrl == null) {
            throw new IllegalStateException("Failed to locate test project " + baseProjectName);
        }
        File projectDir;
        try {
            projectDir = new File(projectUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!projectDir.isDirectory()) {
            throw new IllegalStateException(projectDir + " is not a directory");
        }

        if (testSuffix != null && !testSuffix.isBlank()) {
            var testProjectName = new StringBuilder().append(projectDir.getName());
            if (testSuffix.charAt(0) != '-') {
                testProjectName.append("-");
            }
            testProjectName.append(testSuffix);
            Path targetDir = projectDir.getParentFile().toPath().resolve(testProjectName.toString());
            try {
                IoUtils.copy(projectDir.toPath(), targetDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            projectDir = targetDir.toFile();
        }

        final Properties props = new Properties();
        final File projectProps = new File(projectDir, "gradle.properties");
        if (projectProps.exists()) {
            try (InputStream is = new FileInputStream(projectProps)) {
                props.load(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            props.setProperty("quarkusPlatformGroupId", "io.quarkus");
            props.setProperty("quarkusPlatformArtifactId", "quarkus-bom");
            props.setProperty("org.gradle.logging.level", "INFO");
        }
        final String quarkusVersion = getQuarkusVersion();
        props.setProperty("quarkusPlatformVersion", quarkusVersion);
        props.setProperty("quarkusPluginVersion", quarkusVersion);
        try (OutputStream os = new FileOutputStream(projectProps)) {
            props.store(os, "Quarkus Gradle TS");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return projectDir;
    }

    protected static String getQuarkusVersion() {
        final Path curDir = Paths.get("").toAbsolutePath().normalize();
        final Path gradlePropsFile = curDir.resolve("gradle.properties");
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(gradlePropsFile)) {
            props.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final String quarkusVersion = props.getProperty("version");
        if (quarkusVersion == null) {
            throw new IllegalStateException("Failed to locate Quarkus version in " + gradlePropsFile);
        }
        return quarkusVersion;
    }
}
