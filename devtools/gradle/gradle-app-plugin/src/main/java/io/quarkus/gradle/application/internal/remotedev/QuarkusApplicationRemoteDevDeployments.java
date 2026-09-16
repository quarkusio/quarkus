package io.quarkus.gradle.application.internal.remotedev;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.gradle.api.GradleException;
import org.gradle.deployment.internal.DeploymentRegistry;

public final class QuarkusApplicationRemoteDevDeployments {

    private static final String ID_MARKER = "io.quarkus.application:remote-dev";

    private QuarkusApplicationRemoteDevDeployments() {
    }

    public static AcquiredHandle getOrStart(DeploymentRegistry registry, String id, Parameters parameters) {
        try {
            QuarkusApplicationRemoteDevDeploymentHandle existing = registry.get(id,
                    QuarkusApplicationRemoteDevDeploymentHandle.class);
            if (existing != null) {
                if (!existing.isRunning()) {
                    throw new GradleException("A Quarkus remote-dev session exists for this task but is not running. "
                            + "Stop the current continuous build and restart the remote-dev task.");
                }
                return new AcquiredHandle(existing, false);
            }
            QuarkusApplicationRemoteDevDeploymentHandle started = registry.start(id,
                    DeploymentRegistry.ChangeBehavior.NONE,
                    QuarkusApplicationRemoteDevDeploymentHandle.class, parameters.closeReceiptFile(),
                    parameters.reconnectTriggerFile(), UUID.randomUUID().toString());
            return new AcquiredHandle(started, true);
        } catch (LinkageError e) {
            throw new GradleException("This Gradle version does not expose the internal deployment API expected by "
                    + "Quarkus Gradle-native remote dev.", e);
        }
    }

    public static String deploymentId(Path projectDirectory, String taskPath, String buildName, String configFingerprint) {
        MessageDigest digest = sha256();
        update(digest, ID_MARKER);
        update(digest, projectDirectory.toAbsolutePath().normalize().toString());
        update(digest, taskPath);
        update(digest, buildName);
        update(digest, configFingerprint);
        return "quarkusApplicationRemoteDev-" + hex(digest.digest()).substring(0, 32);
    }

    public static String configFingerprint(Path packageResultFile, Path packageRoot, String remoteUrl) {
        MessageDigest digest = sha256();
        update(digest, packageResultFile.toAbsolutePath().normalize().toString());
        update(digest, packageRoot.toAbsolutePath().normalize().toString());
        update(digest, remoteUrl);
        return hex(digest.digest());
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

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xf, 16));
            builder.append(Character.forDigit(value & 0xf, 16));
        }
        return builder.toString();
    }

    public record Parameters(Path closeReceiptFile, Path reconnectTriggerFile) {
    }

    public record AcquiredHandle(QuarkusApplicationRemoteDevDeploymentHandle handle, boolean started) {
        public QuarkusApplicationRemoteDevSession session() {
            return handle.session();
        }
    }
}
