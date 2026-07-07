package io.quarkus.extension.gradle.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * Package-local artifact and descriptor helpers shared by extension tasks.
 */
final class Util {
    private Util() {
    }

    static String artifactType(ResolvedArtifactResult artifact) {
        return artifact.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
    }

    static String classifier(String artifactId, String version, File file) {
        String artifactIdVersion = version == null || version.isEmpty() || "unspecified".equals(version)
                ? artifactId
                : artifactId + "-" + version;
        if ((file.getName().endsWith(".jar") || file.getName().endsWith(".pom") || file.getName().endsWith(".exe"))
                && file.getName().startsWith(artifactIdVersion + "-")) {
            return file.getName().substring(artifactIdVersion.length() + 1, file.getName().length() - 4);
        }
        return "";
    }

    static boolean isExtension(Path extensionFile) {
        final Path p = extensionFile.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        return Files.exists(p);
    }
}
