package io.quarkus.deployment.pkg.builditem;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.MUTABLE_JAR;

import java.nio.file.Path;
import java.util.Collection;

import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.bootstrap.app.SbomResult;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.sbom.ApplicationManifestConfig;

public final class JarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path originalArtifact;
    private final Path libraryDir;
    private final PackageConfig.JarConfig.JarType type;
    private final String classifier;
    private final ApplicationManifestConfig manifestConfig;

    public JarBuildItem(Path path, Path originalArtifact, Path libraryDir, PackageConfig.JarConfig.JarType type,
            String classifier) {
        this(path, originalArtifact, libraryDir, type, classifier, null);
    }

    public JarBuildItem(Path path, Path originalArtifact, Path libraryDir, PackageConfig.JarConfig.JarType type,
            String classifier, ApplicationManifestConfig manifestConfig) {
        this.path = path;
        this.originalArtifact = originalArtifact;
        this.libraryDir = libraryDir;
        this.type = type;
        this.classifier = classifier;
        this.manifestConfig = manifestConfig;
    }

    public boolean isUberJar() {
        return libraryDir == null;
    }

    public Path getPath() {
        return path;
    }

    public Path getLibraryDir() {
        return libraryDir;
    }

    public Path getOriginalArtifact() {
        return originalArtifact;
    }

    public PackageConfig.JarConfig.JarType getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public ApplicationManifestConfig getManifestConfig() {
        return manifestConfig;
    }

    public JarResult toJarResult() {
        return toJarResult(null);
    }

    public JarResult toJarResult(Collection<SbomResult> sboms) {
        return new JarResult(path, originalArtifact, libraryDir, type == MUTABLE_JAR,
                classifier, sboms);
    }
}
