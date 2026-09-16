package io.quarkus.container.image.docker.common.deployment;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveKind;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

/**
 * Stages and plans the Dockerfile used by Dockerfile-based providers to add a JVM startup archive to an image.
 * <p>
 * The archive is copied into an isolated build context rather than exposing its original host directory to the
 * container-image build. Callers own the returned plan and must close it to remove that temporary context.
 */
public final class StartupArchiveDockerfile {

    private static final Logger LOG = Logger.getLogger(StartupArchiveDockerfile.class);

    private StartupArchiveDockerfile() {
    }

    /**
     * Creates an isolated build context and a Dockerfile that copies and enables the supplied archive.
     *
     * @param stagingParent the directory below which a temporary build context is created
     * @param baseImage the image to enhance
     * @param archive the archive file or directory on the build host
     * @param containerWorkingDirectory the base-image working directory that will contain the archive
     * @param archiveType the archive type and expected filesystem shape
     * @return an owned plan containing the temporary context, exact container archive path, and Dockerfile text
     * @throws IOException if the build context cannot be created or populated
     * @throws IllegalArgumentException if the archive has the wrong shape or its container path contains whitespace
     */
    public static Plan prepare(Path stagingParent, String baseImage, Path archive, String containerWorkingDirectory,
            JvmStartupOptimizerArchiveType archiveType) throws IOException {
        Path normalizedArchive = archive.toAbsolutePath().normalize();
        validateArchive(normalizedArchive, archiveType);

        Path fileName = normalizedArchive.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("JVM startup archive must have a file name: " + normalizedArchive);
        }

        String containerArchive = appendContainerPath(containerWorkingDirectory, fileName.toString());
        if (containerArchive.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "JVM startup archive container path must not contain whitespace: " + containerArchive);
        }
        Files.createDirectories(stagingParent);
        Path contextDirectory = Files.createTempDirectory(stagingParent, ".quarkus-startup-archive-");
        try {
            copyArchive(normalizedArchive, contextDirectory.resolve(fileName));
        } catch (IOException | RuntimeException e) {
            deleteRecursively(contextDirectory);
            throw e;
        }

        String copyDestination = archiveType.getArtifactKind() == JvmStartupOptimizerArchiveKind.DIRECTORY
                ? containerArchive + "/"
                : containerArchive;
        String dockerfile = """
                FROM %s

                # Add the JVM startup archive to its exact runtime location
                COPY ["%s", "%s"]

                # Configure the JVM to consume the startup archive
                ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} %s"
                """.formatted(baseImage, escapeJson(fileName.toString()), escapeJson(copyDestination),
                escapeDockerEnvironmentValue(archiveType.renderRuntimeOption(containerArchive)));
        return new Plan(contextDirectory, containerArchive, dockerfile);
    }

    private static void copyArchive(Path source, Path destination) throws IOException {
        if (Files.isDirectory(source)) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                        throws IOException {
                    Files.createDirectories(destination.resolve(source.relativize(directory)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.copy(source, destination);
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                        throws IOException {
                    if (failure != null) {
                        throw failure;
                    }
                    Files.deleteIfExists(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.debugf(e, "Unable to delete temporary JVM startup archive build context %s", root);
        }
    }

    private static void validateArchive(Path archive, JvmStartupOptimizerArchiveType archiveType) {
        boolean correctShape = switch (archiveType.getArtifactKind()) {
            case FILE -> Files.isRegularFile(archive);
            case DIRECTORY -> Files.isDirectory(archive);
        };
        if (!correctShape) {
            throw new IllegalArgumentException("JVM startup archive " + archive + " is not a "
                    + archiveType.getArtifactKind().name().toLowerCase());
        }
    }

    private static String appendContainerPath(String directory, String fileName) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Container working directory must not be blank");
        }
        return directory.endsWith("/") ? directory + fileName : directory + "/" + fileName;
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u%04x".formatted((int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String escapeDockerEnvironmentValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * An owned Dockerfile build plan.
     *
     * @param contextDirectory the isolated temporary build context containing a snapshot of the archive
     * @param containerArchive the exact file or directory path at which the image will consume the archive
     * @param dockerfile the Dockerfile text to build from {@code contextDirectory}
     */
    public record Plan(Path contextDirectory, String containerArchive, String dockerfile) implements AutoCloseable {

        /**
         * Attempts to recursively remove the temporary build context owned by this plan.
         */
        @Override
        public void close() {
            deleteRecursively(contextDirectory);
        }
    }
}
