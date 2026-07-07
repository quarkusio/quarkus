package io.quarkus.deployment.cmd;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.deployment.pkg.builditem.BuildAotOptimizedContainerImageRequestBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;

/**
 * Adapts build-tool startup-archive context into Quarkus augmentation customizers.
 * <p>
 * The generated customizers declare and produce a
 * {@link BuildAotOptimizedContainerImageRequestBuildItem}. Both the historical AOT-file input and the typed
 * file-or-directory archive input are accepted; when both forms are supplied, they must describe the same AOT file.
 */
public class BuildAotEnhancedCustomizerProducer implements
        Function<Map<String, Object>, Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>>> {

    static final String AOT_FILE = "aot-file";
    static final String STARTUP_ARCHIVE = "startup-archive";
    static final String STARTUP_ARCHIVE_TYPE = "startup-archive-type";

    /**
     * Creates the build-chain and build-execution customizers for one startup-optimized image request.
     *
     * @param context the build-tool command context
     * @return build-chain customizers followed by build-execution customizers
     * @throws IllegalArgumentException if the archive inputs are absent, incomplete, conflicting, or incorrectly typed
     */
    @Override
    public Map.Entry<List<Consumer<BuildChainBuilder>>, List<Consumer<BuildExecutionBuilder>>> apply(
            Map<String, Object> context) {
        String originalContainerImage = (String) context.get("original-container-image");
        String containerWorkingDirectory = (String) context.get("container-working-directory");
        ArchiveRequest archiveRequest = archiveRequest(context);
        return Map.entry(List.of(new BuildChainCustomizer()),
                List.of(new BuildExecutionCustomizer(originalContainerImage, containerWorkingDirectory,
                        archiveRequest.type(), archiveRequest.archive())));
    }

    private static ArchiveRequest archiveRequest(Map<String, Object> context) {
        boolean hasLegacyArchive = context.containsKey(AOT_FILE);
        boolean hasTypedArchive = context.containsKey(STARTUP_ARCHIVE);
        boolean hasTypedArchiveType = context.containsKey(STARTUP_ARCHIVE_TYPE);
        if (hasTypedArchive != hasTypedArchiveType) {
            throw new IllegalArgumentException(
                    "Both 'startup-archive' and 'startup-archive-type' are required for a typed startup archive request");
        }

        if (!hasTypedArchive) {
            if (!hasLegacyArchive) {
                throw new IllegalArgumentException(
                        "Missing startup archive: provide either 'aot-file' or both 'startup-archive' and 'startup-archive-type'");
            }
            return new ArchiveRequest(JvmStartupOptimizerArchiveType.AOT, requirePath(context.get(AOT_FILE), AOT_FILE));
        }

        Path archive = requirePath(context.get(STARTUP_ARCHIVE), STARTUP_ARCHIVE);
        JvmStartupOptimizerArchiveType archiveType = requireArchiveType(context.get(STARTUP_ARCHIVE_TYPE));
        if (hasLegacyArchive) {
            Path legacyArchive = requirePath(context.get(AOT_FILE), AOT_FILE);
            if (archiveType != JvmStartupOptimizerArchiveType.AOT || !archive.equals(legacyArchive)) {
                throw new IllegalArgumentException(
                        "The legacy 'aot-file' and typed startup archive context describe conflicting archives");
            }
        }
        return new ArchiveRequest(archiveType, archive);
    }

    private static Path requirePath(Object value, String contextKey) {
        if (value instanceof Path path) {
            return path;
        }
        throw new IllegalArgumentException("'" + contextKey + "' must be a java.nio.file.Path");
    }

    private static JvmStartupOptimizerArchiveType requireArchiveType(Object value) {
        if (value instanceof JvmStartupOptimizerArchiveType archiveType) {
            return archiveType;
        }
        if (value instanceof String archiveType) {
            try {
                return JvmStartupOptimizerArchiveType.valueOf(archiveType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unsupported 'startup-archive-type' value '" + archiveType + "'", e);
            }
        }
        throw new IllegalArgumentException(
                "'startup-archive-type' must be a JvmStartupOptimizerArchiveType or its enum spelling");
    }

    /**
     * Declares the startup-optimized container-image request as an initial build item.
     */
    public static class BuildChainCustomizer implements Consumer<BuildChainBuilder> {

        /**
         * @param buildChainBuilder the augmentation build chain to customize
         */
        @Override
        public void accept(BuildChainBuilder buildChainBuilder) {
            buildChainBuilder.addInitial(BuildAotOptimizedContainerImageRequestBuildItem.class);
        }
    }

    /**
     * Supplies one typed startup-optimized container-image request to an augmentation execution.
     */
    public static class BuildExecutionCustomizer implements Consumer<BuildExecutionBuilder> {

        private final String originalContainerImage;
        private final String containerWorkingDirectory;
        private final JvmStartupOptimizerArchiveType archiveType;
        private final Path archive;

        /**
         * Creates an execution customizer for an OpenJDK AOT cache.
         *
         * @param originalContainerImage the image to enhance
         * @param containerWorkingDirectory the base-image working directory
         * @param aotFile the AOT cache file on the build host
         */
        public BuildExecutionCustomizer(String originalContainerImage, String containerWorkingDirectory, Path aotFile) {
            this(originalContainerImage, containerWorkingDirectory, JvmStartupOptimizerArchiveType.AOT, aotFile);
        }

        /**
         * Creates an execution customizer for a typed startup archive.
         *
         * @param originalContainerImage the image to enhance
         * @param containerWorkingDirectory the base-image working directory
         * @param archiveType the archive type and expected filesystem shape
         * @param archive the archive file or directory on the build host
         */
        public BuildExecutionCustomizer(String originalContainerImage, String containerWorkingDirectory,
                JvmStartupOptimizerArchiveType archiveType, Path archive) {
            this.originalContainerImage = originalContainerImage;
            this.containerWorkingDirectory = containerWorkingDirectory;
            this.archiveType = archiveType;
            this.archive = archive;
        }

        /**
         * @param buildExecutionBuilder the augmentation execution to customize
         */
        @Override
        public void accept(BuildExecutionBuilder buildExecutionBuilder) {
            buildExecutionBuilder.produce(new BuildAotOptimizedContainerImageRequestBuildItem(originalContainerImage,
                    containerWorkingDirectory, archiveType, archive));
        }
    }

    private record ArchiveRequest(JvmStartupOptimizerArchiveType type, Path archive) {
    }
}
