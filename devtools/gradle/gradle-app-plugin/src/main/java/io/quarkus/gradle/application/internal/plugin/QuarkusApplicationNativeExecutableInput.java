package io.quarkus.gradle.application.internal.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

final class QuarkusApplicationNativeExecutableInput implements CommandLineArgumentProvider, Serializable {

    private static final long serialVersionUID = 1L;

    private final String nativeResultFile;
    private final String suiteName;
    private final String buildName;

    QuarkusApplicationNativeExecutableInput(String nativeResultFile, String suiteName, String buildName) {
        this.nativeResultFile = nativeResultFile;
        this.suiteName = suiteName;
        this.buildName = buildName;
    }

    @Input
    public String getExecutableFingerprint() {
        Path executable = QuarkusApplicationNativeExecutableResolver
                .resolve(Path.of(nativeResultFile), suiteName, buildName);
        try (InputStream input = Files.newInputStream(executable)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName
                    + "' could not fingerprint the native executable for build '" + buildName
                    + "' at " + executable, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public Iterable<String> asArguments() {
        return List.of();
    }
}
