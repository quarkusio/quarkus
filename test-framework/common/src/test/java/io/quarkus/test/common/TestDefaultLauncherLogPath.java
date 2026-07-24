package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the unique log file path logic introduced in the three default artifact launchers
 * ({@link DefaultDockerContainerLauncher}, {@link DefaultJarLauncher}, {@link DefaultNativeImageLauncher})
 * to prevent concurrent {@code @QuarkusIntegrationTest} forks from sharing the same log file.
 */
public class TestDefaultLauncherLogPath {

    // -------------------------------------------------------------------------
    // LauncherUtil.buildUniqueLogPath
    // -------------------------------------------------------------------------

    @Test
    void buildUniqueLogPathInsertsBeforeExtension() {
        Path result = LauncherUtil.buildUniqueLogPath(Path.of("target", "quarkus.log"), "abcde");
        assertThat(result).isEqualTo(Path.of("target", "quarkus-abcde.log"));
    }

    @Test
    void buildUniqueLogPathPreservesCustomBaseFileName() {
        Path result = LauncherUtil.buildUniqueLogPath(Path.of("target", "myapp.log"), "abcde");
        assertThat(result).isEqualTo(Path.of("target", "myapp-abcde.log"));
    }

    @Test
    void buildUniqueLogPathAppendsWhenNoExtension() {
        Path result = LauncherUtil.buildUniqueLogPath(Path.of("target", "quarkus"), "abcde");
        assertThat(result).isEqualTo(Path.of("target", "quarkus-abcde"));
    }

    @Test
    void buildUniqueLogPathWorksWithNoParentDirectory() {
        Path result = LauncherUtil.buildUniqueLogPath(Path.of("quarkus.log"), "abcde");
        assertThat(result).isEqualTo(Path.of("quarkus-abcde.log"));
    }

    // -------------------------------------------------------------------------
    // logFilePath() is null before start()
    // -------------------------------------------------------------------------

    @Test
    void jarLauncherLogFilePathIsNullBeforeStart() {
        assertThat(new DefaultJarLauncher().logFilePath()).isNull();
    }

    @Test
    void nativeImageLauncherLogFilePathIsNullBeforeStart() {
        assertThat(new DefaultNativeImageLauncher().logFilePath()).isNull();
    }

    @Test
    void dockerContainerLauncherLogFilePathIsNullBeforeStart() {
        assertThat(new DefaultDockerContainerLauncher().logFilePath()).isNull();
    }

    // -------------------------------------------------------------------------
    // logFilePath() returns the path that was resolved during start()
    // -------------------------------------------------------------------------

    @Test
    void jarLauncherLogFilePathReturnsResolvedLogFile() throws Exception {
        DefaultJarLauncher launcher = new DefaultJarLauncher();
        Path expected = Path.of("target", "quarkus-test.log");
        setField(launcher, "logFile", expected);
        assertThat(launcher.logFilePath()).isSameAs(expected);
    }

    @Test
    void nativeImageLauncherLogFilePathReturnsResolvedLogFile() throws Exception {
        DefaultNativeImageLauncher launcher = new DefaultNativeImageLauncher();
        Path expected = Path.of("target", "quarkus-test.log");
        setField(launcher, "logFile", expected);
        assertThat(launcher.logFilePath()).isSameAs(expected);
    }

    @Test
    void dockerContainerLauncherLogFilePathReturnsResolvedLogFile() throws Exception {
        DefaultDockerContainerLauncher launcher = new DefaultDockerContainerLauncher();
        Path expected = Path.of("target", "quarkus-test.log");
        setField(launcher, "logFile", expected);
        assertThat(launcher.logFilePath()).isSameAs(expected);
    }

    // -------------------------------------------------------------------------
    // Two launcher instances produce distinct paths
    // -------------------------------------------------------------------------

    @Test
    void twoJarLauncherInstancesHaveDifferentInstanceIds() throws Exception {
        DefaultJarLauncher launcher1 = new DefaultJarLauncher();
        DefaultJarLauncher launcher2 = new DefaultJarLauncher();
        assertThat(getField(launcher1, "instanceId")).isNotEqualTo(getField(launcher2, "instanceId"));
    }

    @Test
    void twoNativeImageLauncherInstancesHaveDifferentInstanceIds() throws Exception {
        DefaultNativeImageLauncher launcher1 = new DefaultNativeImageLauncher();
        DefaultNativeImageLauncher launcher2 = new DefaultNativeImageLauncher();
        assertThat(getField(launcher1, "instanceId")).isNotEqualTo(getField(launcher2, "instanceId"));
    }

    @Test
    void twoJarLauncherInstancesProduceDifferentLogPaths() throws Exception {
        DefaultJarLauncher launcher1 = new DefaultJarLauncher();
        DefaultJarLauncher launcher2 = new DefaultJarLauncher();
        Path base = Path.of("target", "quarkus.log");
        Path path1 = LauncherUtil.buildUniqueLogPath(base, (String) getField(launcher1, "instanceId"));
        Path path2 = LauncherUtil.buildUniqueLogPath(base, (String) getField(launcher2, "instanceId"));
        assertThat(path1).isNotEqualTo(path2);
    }

    // -------------------------------------------------------------------------
    // Docker launcher derives suffix from its unique containerName
    // -------------------------------------------------------------------------

    @Test
    void dockerLauncherContainerNameHasExpectedPrefix() throws Exception {
        DefaultDockerContainerLauncher launcher = new DefaultDockerContainerLauncher();
        String containerName = (String) getField(launcher, "containerName");
        assertThat(containerName).startsWith("quarkus-integration-test-");
    }

    @Test
    void dockerLauncherLogPathUsesContainerNameSuffix() throws Exception {
        DefaultDockerContainerLauncher launcher = new DefaultDockerContainerLauncher();
        String containerName = (String) getField(launcher, "containerName");
        String expectedSuffix = containerName.substring(containerName.lastIndexOf('-') + 1);

        Path result = LauncherUtil.buildUniqueLogPath(Path.of("target", "quarkus.log"), expectedSuffix);
        assertThat(result.getFileName().toString())
                .startsWith("quarkus-")
                .endsWith(".log")
                .contains(expectedSuffix);
    }

    @Test
    void twoDifferentDockerLaunchersHaveDistinctContainerNames() throws Exception {
        DefaultDockerContainerLauncher launcher1 = new DefaultDockerContainerLauncher();
        DefaultDockerContainerLauncher launcher2 = new DefaultDockerContainerLauncher();
        assertThat(getField(launcher1, "containerName")).isNotEqualTo(getField(launcher2, "containerName"));
    }

    // -------------------------------------------------------------------------
    // Additional uniqueness and interface contract tests
    // -------------------------------------------------------------------------

    @Test
    void twoNativeImageLaunchersProduceDifferentLogPaths() throws Exception {
        DefaultNativeImageLauncher launcher1 = new DefaultNativeImageLauncher();
        DefaultNativeImageLauncher launcher2 = new DefaultNativeImageLauncher();
        Path base = Path.of("target", "quarkus.log");
        Path path1 = LauncherUtil.buildUniqueLogPath(base, (String) getField(launcher1, "instanceId"));
        Path path2 = LauncherUtil.buildUniqueLogPath(base, (String) getField(launcher2, "instanceId"));
        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void twoDifferentDockerLaunchersProduceDifferentLogPaths() throws Exception {
        DefaultDockerContainerLauncher launcher1 = new DefaultDockerContainerLauncher();
        DefaultDockerContainerLauncher launcher2 = new DefaultDockerContainerLauncher();
        String cn1 = (String) getField(launcher1, "containerName");
        String cn2 = (String) getField(launcher2, "containerName");
        Path base = Path.of("target", "quarkus.log");
        Path path1 = LauncherUtil.buildUniqueLogPath(base, cn1.substring(cn1.lastIndexOf('-') + 1));
        Path path2 = LauncherUtil.buildUniqueLogPath(base, cn2.substring(cn2.lastIndexOf('-') + 1));
        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    void allThreeLaunchersImplementLogPathProvider() {
        assertThat(new DefaultJarLauncher()).isInstanceOf(LogPathProvider.class);
        assertThat(new DefaultNativeImageLauncher()).isInstanceOf(LogPathProvider.class);
        assertThat(new DefaultDockerContainerLauncher()).isInstanceOf(LogPathProvider.class);
    }

    @Test
    void buildUniqueLogPathWithMultipleDotsInsertsBeforeLastDot() {
        Path result = LauncherUtil.buildUniqueLogPath(Path.of("target", "quarkus.service.log"), "abcde");
        assertThat(result).isEqualTo(Path.of("target", "quarkus.service-abcde.log"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Object getField(Object instance, String fieldName) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void setField(Object instance, String fieldName, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
