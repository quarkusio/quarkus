package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.quarkus.bootstrap.BootstrapConstants;

final class ExtensionDescriptorReader {

    private ExtensionDescriptorReader() {
    }

    static Optional<Properties> readDescriptor(File artifactFile) {
        if (!artifactFile.exists()) {
            return Optional.empty();
        }
        if (artifactFile.isDirectory()) {
            return readDirectoryDescriptor(artifactFile.toPath());
        }
        if (!artifactFile.getName().endsWith(".jar")) {
            return Optional.empty();
        }
        return readJarDescriptor(artifactFile);
    }

    private static Optional<Properties> readDirectoryDescriptor(Path artifactDirectory) {
        Path descriptor = artifactDirectory.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        if (!Files.isRegularFile(descriptor)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(descriptor)) {
            properties.load(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Quarkus extension descriptor " + descriptor, e);
        }
        return Optional.of(properties);
    }

    private static Optional<Properties> readJarDescriptor(File artifactFile) {
        try (ZipFile zip = new ZipFile(artifactFile)) {
            ZipEntry descriptor = zip.getEntry(BootstrapConstants.DESCRIPTOR_PATH);
            if (descriptor == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            try (InputStream input = zip.getInputStream(descriptor)) {
                properties.load(input);
            }
            return Optional.of(properties);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Quarkus extension descriptor from " + artifactFile, e);
        }
    }
}
