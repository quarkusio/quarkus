package io.quarkus.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class QuarkusGradleTestBase {

    protected static File getProjectDir(final String projectName)
            throws URISyntaxException, IOException, FileNotFoundException {
        final URL projectUrl = Thread.currentThread().getContextClassLoader().getResource(projectName);
        if (projectUrl == null) {
            throw new IllegalStateException("Failed to locate test project " + projectName);
        }
        final File projectDir = new File(projectUrl.toURI());
        if (!projectDir.isDirectory()) {
            throw new IllegalStateException(projectDir + " is not a directory");
        }

        final File projectProps = new File(projectDir, "gradle.properties");
        if (!projectProps.exists()) {
            throw new IllegalStateException("Failed to locate " + projectProps);
        }
        final Properties props = new Properties();
        try (InputStream is = new FileInputStream(projectProps)) {
            props.load(is);
        }
        final String quarkusVersion = getQuarkusVersion();
        props.setProperty("quarkusPlatformVersion", quarkusVersion);
        props.setProperty("quarkusPluginVersion", quarkusVersion);
        try (OutputStream os = new FileOutputStream(projectProps)) {
            props.store(os, "Quarkus Gradle TS");
        }
        return projectDir;
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

    protected List<String> arguments(String... argument) {
        List<String> arguments = new ArrayList<>();
        arguments.addAll(Arrays.asList(argument));
        String mavenRepoLocal = System.getProperty("maven.repo.local", System.getenv("MAVEN_LOCAL_REPO"));
        if (mavenRepoLocal != null) {
            arguments.add("-Dmaven.repo.local=" + mavenRepoLocal);
        }
        return arguments;
    }
}
