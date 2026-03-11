package io.quarkus.devtools.mcp;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Quarkus dev mode child processes.
 * Each project directory maps to at most one running instance.
 */
public class QuarkusProcessManager {

    private final ConcurrentHashMap<String, QuarkusInstance> instances = new ConcurrentHashMap<>();

    /**
     * Start a Quarkus app in dev mode for the given project directory.
     */
    public void start(String projectDir, String buildTool) {
        String normalizedDir = normalize(projectDir);

        // Check for existing instance
        QuarkusInstance existing = instances.get(normalizedDir);
        if (existing != null && existing.isAlive()) {
            throw new IllegalStateException("Quarkus instance already running at: " + normalizedDir);
        }

        String detectedBuildTool = buildTool != null ? buildTool : detectBuildTool(normalizedDir);
        ProcessBuilder pb = createProcessBuilder(normalizedDir, detectedBuildTool);

        try {
            Process process = pb.start();
            QuarkusInstance instance = new QuarkusInstance(normalizedDir, process);
            instances.put(normalizedDir, instance);
            System.err.println(
                    "[mcp] Started Quarkus dev mode at: " + normalizedDir + " (build tool: " + detectedBuildTool + ")");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Quarkus dev mode: " + e.getMessage(), e);
        }
    }

    /**
     * Stop the Quarkus app at the given project directory.
     */
    public void stop(String projectDir) {
        String normalizedDir = normalize(projectDir);
        QuarkusInstance instance = instances.get(normalizedDir);
        if (instance == null) {
            throw new IllegalStateException("No Quarkus instance found at: " + normalizedDir);
        }
        instance.stop();
        instances.remove(normalizedDir);
        System.err.println("[mcp] Stopped Quarkus instance at: " + normalizedDir);
    }

    /**
     * Force restart the Quarkus app (sends 's' to dev mode console).
     */
    public void restart(String projectDir) {
        String normalizedDir = normalize(projectDir);
        QuarkusInstance instance = instances.get(normalizedDir);
        if (instance == null) {
            throw new IllegalStateException("No Quarkus instance found at: " + normalizedDir + ". Use quarkus/start first.");
        }

        if (!instance.isAlive()) {
            // Process is dead — need to start a new one
            instances.remove(normalizedDir);
            start(normalizedDir, null);
            System.err.println("[mcp] Re-started dead Quarkus instance at: " + normalizedDir);
        } else {
            instance.restart();
            System.err.println("[mcp] Restart triggered at: " + normalizedDir);
        }
    }

    /**
     * Get the instance for a project directory, or null if not managed.
     */
    public QuarkusInstance getInstance(String projectDir) {
        return instances.get(normalize(projectDir));
    }

    /**
     * Stop all managed instances. Called during MCP server shutdown.
     */
    public void stopAll() {
        for (Map.Entry<String, QuarkusInstance> entry : instances.entrySet()) {
            try {
                entry.getValue().stop();
                System.err.println("[mcp] Stopped instance at: " + entry.getKey());
            } catch (Exception e) {
                System.err.println("[mcp] Error stopping instance at " + entry.getKey() + ": " + e.getMessage());
            }
        }
        instances.clear();
    }

    /**
     * List all managed instances with their status.
     */
    public Map<String, String> listInstances() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, QuarkusInstance> entry : instances.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getStatus().name().toLowerCase());
        }
        return result;
    }

    private String normalize(String projectDir) {
        try {
            return new File(projectDir).getCanonicalPath();
        } catch (IOException e) {
            return new File(projectDir).getAbsolutePath();
        }
    }

    private String detectBuildTool(String projectDir) {
        File dir = new File(projectDir);
        if (new File(dir, "pom.xml").exists()) {
            return "maven";
        } else if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
            return "gradle";
        }
        throw new IllegalArgumentException(
                "Cannot detect build tool at: " + projectDir + ". No pom.xml or build.gradle found.");
    }

    private ProcessBuilder createProcessBuilder(String projectDir, String buildTool) {
        File dir = new File(projectDir);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + projectDir);
        }

        ProcessBuilder pb;
        if ("gradle".equalsIgnoreCase(buildTool)) {
            pb = createGradleProcessBuilder(dir);
        } else {
            pb = createMavenProcessBuilder(dir);
        }

        pb.directory(dir);
        // Don't inherit IO — we manage stdin/stdout/stderr ourselves
        pb.redirectErrorStream(false);
        return pb;
    }

    private ProcessBuilder createMavenProcessBuilder(File projectDir) {
        // Prefer the Maven wrapper if available
        String mvnCmd;
        if (isWindows()) {
            mvnCmd = new File(projectDir, "mvnw.cmd").exists() ? "mvnw.cmd" : "mvn";
        } else {
            mvnCmd = new File(projectDir, "mvnw").exists() ? "./mvnw" : "mvn";
        }
        return new ProcessBuilder(mvnCmd, "quarkus:dev", "-Dquarkus.console.basic=true");
    }

    private ProcessBuilder createGradleProcessBuilder(File projectDir) {
        // Prefer the Gradle wrapper if available
        String gradleCmd;
        if (isWindows()) {
            gradleCmd = new File(projectDir, "gradlew.bat").exists() ? "gradlew.bat" : "gradle";
        } else {
            gradleCmd = new File(projectDir, "gradlew").exists() ? "./gradlew" : "gradle";
        }
        return new ProcessBuilder(gradleCmd, "quarkusDev", "-Dquarkus.console.basic=true");
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
