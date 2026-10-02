package io.quarkus.test.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

/**
 * Describes explicit startup-archive training requested by integration-test artifact metadata.
 * <p>
 * The metadata is optional so Maven, the legacy Gradle plugin and third-party launchers retain their existing
 * auto-detection behavior. When any training property is present, all required values must be supplied together.
 *
 * @param type the archive type to train; only {@link JvmStartupOptimizerArchiveType#AOT AOT} and
 *        {@link JvmStartupOptimizerArchiveType#SCC SCC} are accepted
 * @param destination the absolute host path of the archive file or directory
 * @param executionTarget the environment whose JVM performs the training
 * @param containerDirectory the absolute normalized container directory mounted from the destination's parent;
 *        required only for {@link ExecutionTarget#BASE_IMAGE}
 */
public record JvmStartupArchiveTraining(
        JvmStartupOptimizerArchiveType type,
        Path destination,
        ExecutionTarget executionTarget,
        Optional<String> containerDirectory) {

    /** Metadata property containing the {@link JvmStartupOptimizerArchiveType} enum spelling. */
    public static final String TYPE_PROPERTY = "metadata.jvm-startup-archive.type";

    /** Metadata property containing the absolute host destination path. */
    public static final String DESTINATION_PROPERTY = "metadata.jvm-startup-archive.destination";

    /** Metadata property containing the {@link ExecutionTarget} enum spelling. */
    public static final String EXECUTION_TARGET_PROPERTY = "metadata.jvm-startup-archive.execution-target";

    /** Metadata property containing the container destination directory for base-image training. */
    public static final String CONTAINER_DIRECTORY_PROPERTY = "metadata.jvm-startup-archive.container-directory";

    /**
     * Selects the JVM environment used to train the archive.
     */
    public enum ExecutionTarget {
        /**
         * Train with the JVM that launches the integration-test JAR on the host.
         */
        HOST_JVM,

        /**
         * Train with the JVM supplied by the application container's base image.
         */
        BASE_IMAGE
    }

    /**
     * Validates and normalizes an explicit training request.
     * <p>
     * The destination must be absolute and end with the default name for the selected type. Base-image training
     * requires an absolute normalized container directory without whitespace; host training rejects one.
     */
    public JvmStartupArchiveTraining {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(executionTarget, "executionTarget");
        Objects.requireNonNull(containerDirectory, "containerDirectory");

        if (!destination.isAbsolute()) {
            throw new IllegalArgumentException("The startup-archive destination must be absolute: " + destination);
        }
        destination = destination.normalize();
        if (destination.getParent() == null) {
            throw new IllegalArgumentException("The startup-archive destination must have a parent directory: " + destination);
        }
        if (type == JvmStartupOptimizerArchiveType.AppCDS) {
            throw new IllegalArgumentException("AppCDS is not supported by integration-test startup-archive training");
        }
        String expectedName = type.getDefaultName();
        if (!destination.getFileName().toString().equals(expectedName)) {
            throw new IllegalArgumentException("The " + type + " startup-archive destination must end with '"
                    + expectedName + "': " + destination);
        }
        if (executionTarget == ExecutionTarget.HOST_JVM && containerDirectory.isPresent()) {
            throw new IllegalArgumentException(
                    "Host-JVM startup-archive training must not declare a container directory");
        }
        if (executionTarget == ExecutionTarget.BASE_IMAGE) {
            String directory = containerDirectory
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Base-image startup-archive training requires a container directory"));
            validateContainerDirectory(directory);
        }
    }

    /**
     * Parses an optional training request from integration-test artifact metadata.
     *
     * @param properties the artifact metadata
     * @return the parsed request, or an empty optional when no startup-archive properties are present
     * @throws IllegalArgumentException if metadata is incomplete or violates the typed training contract
     */
    public static Optional<JvmStartupArchiveTraining> fromMetadata(Properties properties) {
        Optional<String> type = property(properties, TYPE_PROPERTY);
        Optional<String> destination = property(properties, DESTINATION_PROPERTY);
        Optional<String> executionTarget = property(properties, EXECUTION_TARGET_PROPERTY);
        Optional<String> containerDirectory = property(properties, CONTAINER_DIRECTORY_PROPERTY);
        if (type.isEmpty() && destination.isEmpty() && executionTarget.isEmpty() && containerDirectory.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new JvmStartupArchiveTraining(
                    parseArchiveType(required(type, TYPE_PROPERTY)),
                    Path.of(required(destination, DESTINATION_PROPERTY)),
                    ExecutionTarget.valueOf(required(executionTarget, EXECUTION_TARGET_PROPERTY)),
                    containerDirectory));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JVM startup-archive training metadata: " + e.getMessage(), e);
        }
    }

    /**
     * @return the host directory containing the archive destination
     */
    public Path hostDirectory() {
        return destination.getParent();
    }

    /**
     * @return the absolute archive path visible inside the training container
     * @throws java.util.NoSuchElementException when this is not a base-image training request
     */
    public String containerArchivePath() {
        return containerDirectory.orElseThrow() + "/" + destination.getFileName();
    }

    /**
     * @return the host destination for the intermediate OpenJDK AOT recording configuration
     * @throws IllegalStateException when the archive type is not {@link JvmStartupOptimizerArchiveType#AOT}
     */
    public Path aotConfigurationDestination() {
        requireType(JvmStartupOptimizerArchiveType.AOT);
        return destination.resolveSibling("app.aotconf");
    }

    /**
     * @return the path of the intermediate OpenJDK AOT recording configuration inside the training container
     * @throws IllegalStateException when the archive type is not {@link JvmStartupOptimizerArchiveType#AOT}
     * @throws java.util.NoSuchElementException when this is not a base-image training request
     */
    public String containerAotConfigurationPath() {
        requireType(JvmStartupOptimizerArchiveType.AOT);
        return containerDirectory.orElseThrow() + "/app.aotconf";
    }

    /**
     * Creates the host output directory and removes output left by an earlier training run.
     * <p>
     * For AOT training this removes the archive and intermediate recording configuration. For SCC training it
     * recreates an empty destination directory.
     *
     * @throws IOException if the output cannot be prepared
     */
    public void prepareHostOutput() throws IOException {
        prepareHostOutput(false);
    }

    /**
     * Prepares fresh host output that will be mounted into a training container.
     * <p>
     * In addition to {@link #prepareHostOutput()}, this attempts to make the mounted host directories readable,
     * writable, and searchable by any user so a container JVM running under a different user can populate them.
     *
     * @throws IOException if the output cannot be prepared
     */
    public void prepareContainerMountedHostOutput() throws IOException {
        prepareHostOutput(true);
    }

    /**
     * Verifies that training produced a non-empty archive of the shape required by {@link #type()}.
     *
     * @throws IllegalStateException if the result is absent, empty, has the wrong shape, or cannot be inspected
     */
    public void validateProducedArchive() {
        try {
            switch (type.getArtifactKind()) {
                case FILE -> {
                    if (!Files.isRegularFile(destination) || Files.size(destination) == 0) {
                        throw new IllegalStateException(
                                type + " startup-archive training did not produce a non-empty file at " + destination);
                    }
                }
                case DIRECTORY -> {
                    if (!Files.isDirectory(destination)) {
                        throw new IllegalStateException(
                                type + " startup-archive training did not produce a directory at " + destination);
                    }
                    try (Stream<Path> entries = Files.list(destination)) {
                        if (entries.findAny().isEmpty()) {
                            throw new IllegalStateException(
                                    type + " startup-archive training produced an empty directory at " + destination);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect startup-archive training result at " + destination, e);
        }
    }

    private void requireType(JvmStartupOptimizerArchiveType expected) {
        if (type != expected) {
            throw new IllegalStateException("Operation requires startup-archive type " + expected + " but was " + type);
        }
    }

    private void prepareHostOutput(boolean containerMounted) throws IOException {
        Files.createDirectories(hostDirectory());
        if (containerMounted) {
            makeWorldAccessible(hostDirectory());
        }
        if (type == JvmStartupOptimizerArchiveType.AOT) {
            Files.deleteIfExists(destination);
            Files.deleteIfExists(aotConfigurationDestination());
        } else {
            deleteDirectoryContents(destination);
            Files.createDirectories(destination);
            if (containerMounted) {
                makeWorldAccessible(destination);
            }
        }
    }

    private static void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path visitedDirectory, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                if (!visitedDirectory.equals(directory)) {
                    Files.delete(visitedDirectory);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void makeWorldAccessible(Path path) {
        path.toFile().setReadable(true, false);
        path.toFile().setWritable(true, false);
        path.toFile().setExecutable(true, false);
    }

    private static Optional<String> property(Properties properties, String name) {
        return Optional.ofNullable(properties.getProperty(name)).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static String required(Optional<String> value, String name) {
        return value.orElseThrow(() -> new IllegalArgumentException("Missing property '" + name + "'"));
    }

    private static JvmStartupOptimizerArchiveType parseArchiveType(String value) {
        return value.equals("APP_CDS") ? JvmStartupOptimizerArchiveType.AppCDS
                : JvmStartupOptimizerArchiveType.valueOf(value);
    }

    private static void validateContainerDirectory(String directory) {
        if (!directory.startsWith("/") || directory.endsWith("/") || directory.contains("//")
                || directory.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "The startup-archive container directory must be an absolute normalized container path without whitespace: "
                            + directory);
        }
        for (String element : directory.substring(1).split("/")) {
            if (element.isEmpty() || element.equals(".") || element.equals("..")) {
                throw new IllegalArgumentException(
                        "The startup-archive container directory must be an absolute normalized container path: "
                                + directory);
            }
        }
    }
}
