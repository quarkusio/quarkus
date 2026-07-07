package io.quarkus.gradle.model.pom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.util.HashUtil;

/**
 * Shared helpers for translating Gradle-resolved files and workspace outputs into the Quarkus application model.
 * <p>
 * This is implementation API shared by the legacy and standalone Gradle model builders. It does not resolve
 * dependencies and is not a user-facing extension point.
 */
public final class ApplicationModelBuilderSupport {

    private ApplicationModelBuilderSupport() {
    }

    /**
     * Tests whether all bits in {@code flag} are enabled in {@code walkingFlags}.
     *
     * @param walkingFlags current traversal flags
     * @param flag bits to test
     * @return whether the requested bits are enabled
     */
    public static boolean isFlagOn(byte walkingFlags, byte flag) {
        return (walkingFlags & flag) > 0;
    }

    /**
     * Clears the supplied bits when they are enabled.
     *
     * @param flags current flags
     * @param flag bits to clear
     * @return the resulting flag set
     */
    public static byte clearFlag(byte flags, byte flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
        return flags;
    }

    /**
     * Adds existing, unique output directories from {@code sources} to {@code paths}, preserving encounter order.
     *
     * @param sources workspace source directories whose output directories are collected
     * @param paths destination path builder
     */
    public static void collectDestinationDirs(Collection<SourceDir> sources, PathList.Builder paths) {
        for (SourceDir src : sources) {
            Path path = src.getOutputDir();
            if (paths.contains(path) || !Files.exists(path)) {
                continue;
            }
            paths.add(path);
        }
    }

    /**
     * Adds each existing file or directory dependency to the application model.
     * <p>
     * Quarkus extension descriptors in those paths are processed before the dependency is added. Missing paths are
     * ignored.
     *
     * @param modelBuilder application model to update
     * @param fileDependencies resolved file or directory dependencies
     */
    public static void addFileDependencies(ApplicationModelBuilder modelBuilder, Set<File> fileDependencies) {
        for (File file : fileDependencies) {
            if (!file.exists()) {
                continue;
            }
            ResolvedDependencyBuilder artifactBuilder = toFileDependency(file);
            processQuarkusDependency(artifactBuilder, modelBuilder);
            modelBuilder.addDependency(artifactBuilder);
        }
    }

    /**
     * Creates synthetic coordinates for a file dependency.
     * <p>
     * The coordinates are an internal identity derived from the parent path, file name/type, and last-modified time;
     * they are not Maven coordinates and must not be persisted as a publication contract.
     *
     * @param file resolved file or directory dependency
     * @return initialized dependency builder
     */
    public static ResolvedDependencyBuilder toFileDependency(File file) {
        String parentPath = file.getParent();
        String group = HashUtil.sha1(parentPath == null ? file.getName() : parentPath);
        String name = file.getName();
        String type = ArtifactCoords.TYPE_JAR;
        if (!file.isDirectory()) {
            int dot = file.getName().lastIndexOf('.');
            if (dot > 0) {
                name = file.getName().substring(0, dot);
                type = file.getName().substring(dot + 1);
            }
        }
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(group)
                .setArtifactId(name)
                .setType(type)
                .setVersion(String.valueOf(file.lastModified()))
                .setResolvedPath(file.toPath())
                .setDirect(true)
                .setRuntimeCp()
                .setDeploymentCp();
    }

    /**
     * Inspects a JAR or directory for Quarkus extension metadata and applies it to the dependency and model builders.
     *
     * @param artifactBuilder dependency to inspect and update
     * @param modelBuilder application model that receives extension metadata
     * @return {@code true} when an extension descriptor was found and processed
     */
    public static boolean processQuarkusDependency(ResolvedDependencyBuilder artifactBuilder,
            ApplicationModelBuilder modelBuilder) {
        if (!artifactBuilder.getType().equals(ArtifactCoords.TYPE_JAR)) {
            return false;
        }
        for (Path artifactPath : artifactBuilder.getResolvedPaths()) {
            if (!Files.exists(artifactPath)) {
                continue;
            }
            if (Files.isDirectory(artifactPath)) {
                if (processQuarkusDir(artifactBuilder, artifactPath.resolve(BootstrapConstants.META_INF), modelBuilder)) {
                    return true;
                }
                continue;
            }
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactPath)) {
                if (processQuarkusDir(artifactBuilder, artifactFs.getPath(BootstrapConstants.META_INF), modelBuilder)) {
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to process " + artifactPath, e);
            }
        }
        return false;
    }

    private static boolean processQuarkusDir(ResolvedDependencyBuilder artifactBuilder, Path quarkusDir,
            ApplicationModelBuilder modelBuilder) {
        if (!Files.exists(quarkusDir)) {
            return false;
        }
        Path quarkusDescr = quarkusDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
        if (!Files.exists(quarkusDescr)) {
            return false;
        }
        Properties extProps = readDescriptor(quarkusDescr);
        if (extProps == null) {
            return false;
        }
        artifactBuilder.setRuntimeExtensionArtifact();
        modelBuilder.handleExtensionProperties(extProps, artifactBuilder.getKey());

        String providesCapabilities = extProps.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
        if (providesCapabilities != null) {
            modelBuilder.addExtensionCapabilities(
                    CapabilityContract.of(artifactBuilder.toGACTVString(), providesCapabilities, null));
        }
        return true;
    }

    private static Properties readDescriptor(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        Properties rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load extension description " + path, e);
        }
        return rtProps;
    }
}
