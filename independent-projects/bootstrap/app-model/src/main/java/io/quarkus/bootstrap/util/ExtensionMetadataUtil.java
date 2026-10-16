package io.quarkus.bootstrap.util;

import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;

/**
 * Utility for reading extension metadata from path trees,
 * preferring the JSON format and falling back to YAML.
 */
public final class ExtensionMetadataUtil {

    private ExtensionMetadataUtil() {
    }

    /**
     * Applies a function to the extension metadata in the given path tree,
     * trying JSON first and falling back to YAML.
     *
     * @param <T> the result type
     * @param tree the path tree to search
     * @param func the function to apply to the metadata path visit
     * @return the result of the function, or null if no metadata was found
     */
    public static <T> T applyExtensionMetadata(PathTree tree, Function<PathVisit, T> func) {
        return tree.apply(BootstrapConstants.EXTENSION_JSON_METADATA_PATH, visit -> {
            if (visit != null) {
                return func.apply(visit);
            }
            return tree.apply(BootstrapConstants.EXTENSION_METADATA_PATH, yamlVisit -> {
                if (yamlVisit != null) {
                    return func.apply(yamlVisit);
                }
                return null;
            });
        });
    }

    /**
     * Consumes extension metadata from the given path tree,
     * trying JSON first and falling back to YAML.
     * The consumer receives a non-null visit if metadata was found,
     * or null if neither format was found.
     *
     * @param tree the path tree to search
     * @param consumer the consumer to process the metadata
     */
    public static void acceptExtensionMetadata(PathTree tree, Consumer<PathVisit> consumer) {
        tree.accept(BootstrapConstants.EXTENSION_JSON_METADATA_PATH, visit -> {
            if (visit != null) {
                consumer.accept(visit);
            } else {
                tree.accept(BootstrapConstants.EXTENSION_METADATA_PATH, yamlVisit -> {
                    if (yamlVisit != null) {
                        consumer.accept(yamlVisit);
                    }
                });
            }
        });
    }
}
