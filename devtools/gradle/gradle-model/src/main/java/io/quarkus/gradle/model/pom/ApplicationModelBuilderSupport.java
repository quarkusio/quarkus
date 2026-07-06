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

public final class ApplicationModelBuilderSupport {

    private ApplicationModelBuilderSupport() {
    }

    public static boolean isFlagOn(byte walkingFlags, byte flag) {
        return (walkingFlags & flag) > 0;
    }

    public static byte clearFlag(byte flags, byte flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
        return flags;
    }

    public static void collectDestinationDirs(Collection<SourceDir> sources, PathList.Builder paths) {
        for (SourceDir src : sources) {
            Path path = src.getOutputDir();
            if (paths.contains(path) || !Files.exists(path)) {
                continue;
            }
            paths.add(path);
        }
    }

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

    public static boolean processQuarkusDependency(ResolvedDependencyBuilder artifactBuilder,
            ApplicationModelBuilder modelBuilder) {
        for (Path artifactPath : artifactBuilder.getResolvedPaths()) {
            if (!Files.exists(artifactPath) || !artifactBuilder.getType().equals(ArtifactCoords.TYPE_JAR)) {
                break;
            }
            if (Files.isDirectory(artifactPath)) {
                return processQuarkusDir(artifactBuilder, artifactPath.resolve(BootstrapConstants.META_INF), modelBuilder);
            }
            try (FileSystem artifactFs = ZipUtils.newFileSystem(artifactPath)) {
                return processQuarkusDir(artifactBuilder, artifactFs.getPath(BootstrapConstants.META_INF), modelBuilder);
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
