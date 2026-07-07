package io.quarkus.gradle.application.internal.dev;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.gradle.api.GradleException;
import org.gradle.deployment.internal.DeploymentRegistry;

/**
 * Adapter boundary for Gradle's internal deployment API. Keep interaction with
 * {@code org.gradle.deployment.internal.*} here and in
 * {@link QuarkusApplicationDevDeploymentHandle}.
 */
public final class QuarkusApplicationDevDeployments {

    private static final String ID_MARKER = "io.quarkus.application:quarkusApplicationDev";

    private QuarkusApplicationDevDeployments() {
    }

    public static AcquiredHandle getOrStart(DeploymentRegistry registry, String id, Parameters parameters) {
        try {
            QuarkusApplicationDevDeploymentHandle existing = registry.get(id, QuarkusApplicationDevDeploymentHandle.class);
            if (existing != null) {
                if (!existing.configFingerprint().equals(parameters.configFingerprint())) {
                    throw new GradleException("A Quarkus dev session is already running for this task with different "
                            + "configuration. Stop the current continuous build and restart this task.");
                }
                return new AcquiredHandle(existing, existing.acquire());
            }
            QuarkusApplicationDevDeploymentHandle started = registry.start(id, DeploymentRegistry.ChangeBehavior.NONE,
                    QuarkusApplicationDevDeploymentHandle.class, parameters.configFingerprint(),
                    parameters.launchParameters(), parameters.closeReceiptFile(), parameters.replayTriggerFile(),
                    UUID.randomUUID().toString());
            return new AcquiredHandle(started, Acquisition.STARTED_INITIAL);
        } catch (LinkageError e) {
            throw new GradleException("This Gradle version does not expose the internal deployment API expected by "
                    + "Quarkus Gradle-native dev mode.", e);
        }
    }

    public static String deploymentId(Path projectDirectory, String taskPath) {
        MessageDigest digest = sha256();
        update(digest, ID_MARKER);
        update(digest, projectDirectory.toAbsolutePath().normalize().toString());
        update(digest, taskPath);
        return "quarkusApplicationDev-" + hex(digest.digest()).substring(0, 32);
    }

    public static String configFingerprint(GradleNativeDevModeLauncher.Parameters parameters, Path replayTriggerFile) {
        MessageDigest digest = sha256();
        update(digest, parameters.javaExecutable().toString());
        update(digest, parameters.applicationModel().toAbsolutePath().normalize().toString());
        update(digest, parameters.testApplicationModel() == null ? "" : parameters.testApplicationModel().toString());
        update(digest, Boolean.toString(parameters.continuousTestOnly()));
        update(digest, Boolean.toString(parameters.continuousTesting()));
        update(digest, parameters.projectDirectory().toAbsolutePath().normalize().toString());
        update(digest, parameters.buildDirectory().toAbsolutePath().normalize().toString());
        update(digest, replayTriggerFile.toAbsolutePath().normalize().toString());
        update(digest, parameters.workingDirectory().toString());
        update(digest, parameters.applicationName());
        update(digest, parameters.applicationVersion());
        updatePaths(digest, "devModeClasspath", parameters.devModeClasspath());
        updatePaths(digest, "testSourceDirectories", parameters.testSourceDirectories());
        updatePaths(digest, "testClasses", parameters.testClasses());
        updatePaths(digest, "testResources", parameters.testResources());
        sortedEntries(parameters.quarkusBuildProperties())
                .forEach(entry -> update(digest, entry.getKey() + "=" + entry.getValue()));
        update(digest, "devJvmArgs");
        parameters.devJvmArgs().forEach(arg -> update(digest, arg));
        update(digest, "jvmArguments");
        parameters.jvmArguments().forEach(arg -> update(digest, arg));
        update(digest, "applicationArguments");
        parameters.applicationArguments().forEach(arg -> update(digest, arg));
        update(digest, "modules");
        parameters.modules().forEach(arg -> update(digest, arg));
        update(digest, "openJavaLang");
        update(digest, Boolean.toString(parameters.openJavaLang()));
        update(digest, "compilerArguments");
        parameters.compilerArguments().forEach(arg -> update(digest, arg));
        update(digest, "tests");
        parameters.tests().forEach(arg -> update(digest, arg));
        update(digest, "forcePlainConsole");
        update(digest, Boolean.toString(parameters.forcePlainConsole()));
        sortedEntries(parameters.devSystemProperties())
                .forEach(entry -> update(digest, entry.getKey() + "=" + entry.getValue()));
        update(digest, "environmentVariables");
        update(digest, environmentFingerprint(parameters.environmentVariables()));
        updateNullable(digest, "debug", parameters.debug());
        updateNullable(digest, "debugMode", parameters.debugMode());
        updateNullable(digest, "debugHost", parameters.debugHost());
        updateNullable(digest, "debugPort", parameters.debugPort());
        updateNullable(digest, "suspend", parameters.suspend());
        updateNullable(digest, "forceC2", parameters.forceC2());
        update(digest, "disableAllExtensionJvmOptions");
        update(digest, Boolean.toString(parameters.disableAllExtensionJvmOptions()));
        update(digest, "disableExtensionJvmOptionsFor");
        parameters.disableExtensionJvmOptionsFor().stream()
                .sorted()
                .forEach(pattern -> update(digest, pattern));
        return hex(digest.digest());
    }

    public static String environmentFingerprint(Map<String, String> environmentVariables) {
        MessageDigest digest = sha256();
        update(digest, "io.quarkus.application:dev-environment");
        for (Map.Entry<String, String> entry : sortedEntries(environmentVariables)) {
            update(digest, entry.getKey());
            update(digest, entry.getValue());
        }
        return hex(digest.digest());
    }

    private static List<String> sortedPaths(Iterable<File> files) {
        List<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.toPath().toAbsolutePath().normalize().toString());
        }
        paths.sort(Comparator.naturalOrder());
        return paths;
    }

    private static void updatePaths(MessageDigest digest, String name, Iterable<File> files) {
        List<String> paths = sortedPaths(files);
        update(digest, name);
        update(digest, Integer.toString(paths.size()));
        paths.forEach(path -> update(digest, path));
    }

    private static List<Map.Entry<String, String>> sortedEntries(Map<String, String> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static void updateNullable(MessageDigest digest, String name, Object value) {
        update(digest, name);
        if (value == null) {
            update(digest, "unset");
            return;
        }
        update(digest, "set");
        update(digest, value.toString());
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xf, 16));
            builder.append(Character.forDigit(value & 0xf, 16));
        }
        return builder.toString();
    }

    public record Parameters(
            String configFingerprint,
            GradleNativeDevModeLauncher.Parameters launchParameters,
            Path closeReceiptFile,
            Path replayTriggerFile) {
    }

    public record AcquiredHandle(
            QuarkusApplicationDevDeploymentHandle handle,
            Acquisition acquisition) {

        public boolean started() {
            return acquisition != Acquisition.EXISTING_READY;
        }

        public boolean restarted() {
            return acquisition == Acquisition.RESTARTED_AFTER_FAILURE;
        }
    }

    public enum Acquisition {
        EXISTING_READY,
        STARTED_INITIAL,
        RESTARTED_AFTER_FAILURE
    }
}
