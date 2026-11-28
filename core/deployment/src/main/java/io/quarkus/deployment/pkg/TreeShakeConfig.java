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
     * Dependency classes to keep regardless of reachability analysis, expressed as fully qualified
     * binary class names, e.g. {@code com.example.Outer$Inner} for a member class.
     * <p>
     * Each class is treated as a root of the reachability analysis: the classes it references,
     * its supertypes and its member classes are kept as well.
     * <p>
     * This is a finer-grained alternative to {@code excluded-artifacts} for classes that are loaded in
     * ways the tree-shaker cannot detect. A class that isn't a candidate for removal, such as an application
     * class, a class of an excluded artifact or a class that isn't on the runtime classpath, is
     * reported with a warning.
     */
    Optional<Set<String>> keepClasses();

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
