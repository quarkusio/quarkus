package io.quarkus.maven.toolchain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Resolves the JDK configured through Maven Toolchains.
 */
public final class MavenToolchains {

    private static final String FORK_BUILD_DISABLED = "quarkus.maven.fork-build.disabled";

    private MavenToolchains() {
    }

    public static Optional<Toolchain> findJdkToolchain(ToolchainManager toolchainManager, MavenSession session) {
        if (toolchainManager == null || session == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toolchainManager.getToolchainFromBuildContext("jdk", session));
    }

    public static Optional<String> findJavaExecutable(ToolchainManager toolchainManager, MavenSession session) {
        return findJdkToolchain(toolchainManager, session).map(toolchain -> toolchain.findTool("java"));
    }

    public static Optional<Path> findJavaHome(ToolchainManager toolchainManager, MavenSession session) {
        return findJavaExecutable(toolchainManager, session).flatMap(MavenToolchains::javaHomeFromExecutable);
    }

    /**
     * Returns {@code true} when augmentation/code generation should run in a JVM forked from the configured toolchain.
     */
    public static boolean shouldFork(ToolchainManager toolchainManager, MavenSession session) {
        if (Boolean.getBoolean(FORK_BUILD_DISABLED)) {
            return false;
        }
        Optional<Path> toolchainHome = findJavaHome(toolchainManager, session);
        if (toolchainHome.isEmpty()) {
            return false;
        }
        try {
            Path currentHome = Path.of(System.getProperty("java.home")).toRealPath();
            return !currentHome.equals(toolchainHome.get().toRealPath());
        } catch (IOException e) {
            return true;
        }
    }

    private static Optional<Path> javaHomeFromExecutable(String javaExecutable) {
        if (javaExecutable == null || javaExecutable.isBlank()) {
            return Optional.empty();
        }
        Path javaPath = Path.of(javaExecutable);
        Path javaHome = javaPath.getParent();
        if (javaHome == null) {
            return Optional.empty();
        }
        javaHome = javaHome.getParent();
        return javaHome == null ? Optional.empty() : Optional.of(javaHome);
    }
}
