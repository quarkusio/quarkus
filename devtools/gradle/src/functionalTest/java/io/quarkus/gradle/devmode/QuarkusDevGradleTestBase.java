package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import io.quarkus.gradle.QuarkusGradleTestBase;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public abstract class QuarkusDevGradleTestBase extends QuarkusGradleTestBase {

    private static final String PLUGIN_UNDER_TEST_METADATA_PROPERTIES = "plugin-under-test-metadata.properties";

    private Future<?> quarkusDev;
    private File projectDir;

    @Test
    public void main() throws Exception {

        projectDir = getProjectDir();
        ExecutorService executor = null;
        try {
            executor = Executors.newSingleThreadExecutor();
            quarkusDev = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        build();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to build the project", e);
                    }
                }
            });
            testDevMode();
        } finally {
            if (quarkusDev != null) {
                quarkusDev.cancel(true);
            }
            if(executor != null) {
                executor.shutdownNow();
            }

            // Kill all process using the live reload and the live reload process.
            DevModeTestUtils.killProcesses("quarkusDev", projectDir.toString());

            DevModeTestUtils.awaitUntilServerDown();

            if (projectDir != null && projectDir.isDirectory()) {
                FileUtils.deleteDirectory(projectDir);
            }
        }
    }

    protected void build() throws Exception {
        // Plugin's classpath won't be visible in QuarkusDev task
        // so, here we are going to propagate the plugin-under-test-metadata properties
        final Path path = ClassPathUtils
                .toLocalPath(Thread.currentThread().getContextClassLoader().getResource(PLUGIN_UNDER_TEST_METADATA_PROPERTIES));
        if (!Files.exists(path)) {
            throw new IllegalStateException("Failed to locate " + PLUGIN_UNDER_TEST_METADATA_PROPERTIES + " on the classpath");
        }
        System.setProperty(PLUGIN_UNDER_TEST_METADATA_PROPERTIES, path.toAbsolutePath().toString());

        final BuildResult buildResult = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments(buildArguments()))
                .withProjectDir(projectDir)
                .build();
        assertBuildResult(buildResult);
    }

    protected abstract String projectDirectoryName();

    protected File getProjectDir() {
        if(projectDir == null) {
            final String projectDirName = projectDirectoryName();
            try {
                final File projectDir = Files.createTempDirectory(projectDirName).toFile();
                FileUtils.copyDirectory(getProjectDir(projectDirName), projectDir);
                this.projectDir = projectDir;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create a project dir for " + projectDirName, e);
            }
        }
        return projectDir;
    }

    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "-s" };
    }

    protected void assertBuildResult(BuildResult buildResult) {
    }

    protected abstract void testDevMode() throws Exception;

    protected String getHttpResponse() {
        return DevModeTestUtils.getHttpResponse(getQuarkusDevBrokenReason());
    }

    protected String getHttpResponse(String path) {
        return DevModeTestUtils.getHttpResponse(path, getQuarkusDevBrokenReason());
    }

    private Supplier<String> getQuarkusDevBrokenReason() {
        return () -> {
            return quarkusDev == null ? null : quarkusDev.isDone() ? "quarkusDev mode has terminated" : null;
        };
    }

    protected void replace(String srcFile, Map<String, String> tokens) {
        final File source = new File(getProjectDir(), srcFile);
        assertThat(source).exists();
        try {
            DevModeTestUtils.filter(source, tokens);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to replace tokens in " + source, e);
        }
    }

    protected void assertUpdatedResponseContains(String path, String value) {
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> getHttpResponse(path).contains(value));
    }
}
