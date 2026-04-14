package io.quarkus.deployment.pkg;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Tree-shaking configuration.
 */
@ConfigGroup
public interface TreeShakeConfig {

    /**
     * Whether to perform class reachability analysis and exclude non-reachable classes
     * from the produced JAR.
     * <ul>
     * <li>{@code none} - No analysis or exclusion is performed.</li>
     * <li>{@code classes} - Exclude non-reachable classes from dependencies.</li>
     * </ul>
     */
    @WithDefault("none")
    TreeShakeMode mode();

    /**
     * Dependency artifacts to exclude from tree-shaking. All classes from excluded
     * artifacts are preserved regardless of reachability analysis.
     * <p>
     * This is useful for libraries that perform self-integrity checks (e.g., BouncyCastle FIPS)
     * or that load classes in ways the tree-shaker cannot detect.
     * <p>
     * Each dependency is expressed as {@code groupId:artifactId[:[classifier][:[type]]]}.
     * The classifier and type are optional. If the type is missing, {@code jar} is assumed.
     */
    Optional<Set<String>> excludedArtifacts();

    /**
     * Tree shaking mode.
     */
    enum TreeShakeMode {
        /**
         * No analysis or exclusion is performed.
         */
        NONE,
        /**
         * Exclude non-reachable classes from dependencies.
         */
        CLASSES;
    }
}
