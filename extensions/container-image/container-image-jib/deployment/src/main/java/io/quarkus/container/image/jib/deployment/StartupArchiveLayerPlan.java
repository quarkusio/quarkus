package io.quarkus.container.image.jib.deployment;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;

import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageRequestBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

/**
 * Keeps Jib's host archive source, container placement, and JVM option in one validated plan.
 * <p>
 * Jib consumes the original file or directory directly as a layer source; unlike Dockerfile providers, this plan
 * does not own a staging copy.
 */
record StartupArchiveLayerPlan(Path archive, AbsoluteUnixPath destinationDirectory,
        AbsoluteUnixPath containerArchive, String runtimeOption) {

    static StartupArchiveLayerPlan from(BuildAotOptimizedContainerImageRequestBuildItem request) {
        return from(request.getArchive(), request.getContainerWorkingDirectory(), request.getArchiveType());
    }

    static StartupArchiveLayerPlan from(Path archive, String containerWorkingDirectory,
            JvmStartupOptimizerArchiveType archiveType) {
        Path normalizedArchive = archive.toAbsolutePath().normalize();
        boolean correctShape = switch (archiveType.getArtifactKind()) {
            case FILE -> Files.isRegularFile(normalizedArchive);
            case DIRECTORY -> Files.isDirectory(normalizedArchive);
        };
        if (!correctShape) {
            throw new IllegalArgumentException("JVM startup archive " + normalizedArchive + " is not a "
                    + archiveType.getArtifactKind().name().toLowerCase());
        }

        AbsoluteUnixPath destinationDirectory = AbsoluteUnixPath.get(containerWorkingDirectory);
        AbsoluteUnixPath containerArchive = destinationDirectory.resolve(normalizedArchive.getFileName());
        if (containerArchive.toString().chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "JVM startup archive container path must not contain whitespace: " + containerArchive);
        }
        return new StartupArchiveLayerPlan(normalizedArchive, destinationDirectory, containerArchive,
                archiveType.renderRuntimeOption(containerArchive.toString()));
    }

    /**
     * Treat any user-supplied option for the selected mechanism as authoritative. In particular, every
     * {@code -Xshareclasses} option suppresses the generated SCC option because combining independently configured
     * OpenJ9 cache directives would change the user's cache semantics.
     */
    static boolean containsRuntimeOption(Iterable<String> arguments, JvmStartupOptimizerArchiveType archiveType) {
        String prefix = archiveType == JvmStartupOptimizerArchiveType.SCC ? "-Xshareclasses" : archiveType.getJvmFlag();
        for (String argument : arguments) {
            if (argument.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
