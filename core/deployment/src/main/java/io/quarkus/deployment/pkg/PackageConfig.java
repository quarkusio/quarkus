package io.quarkus.deployment.pkg;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Packaging the application
 * <p>
 * Configuration relating to creating a packaged output.
 */
@ConfigMapping(prefix = "quarkus.package")
@ConfigRoot
public interface PackageConfig {
    /**
     * Configuration which applies to building a JAR file for the project.
     */
    JarConfig jar();

    /**
     * The entry point of the application. This can either be a fully qualified name of a standard Java
     * class with a main method, or {@link io.quarkus.runtime.QuarkusApplication}.
     * <p>
     * If your application has main classes annotated with {@link io.quarkus.runtime.annotations.QuarkusMain}
     * then this can also reference the name given in the annotation, to avoid the need to specify fully qualified
     * names in the config.
     */
    Optional<String> mainClass();

    /**
     * The directory into which the output package(s) should be written.
     * Relative paths are resolved from the build systems target directory.
     */
    Optional<Path> outputDirectory();

    /**
     * The name of the final artifact, excluding the suffix and file extension.
     */
    Optional<String> outputName();

    /**
     * Setting this switch to {@code true} will cause Quarkus to write the transformed application bytecode
     * to the build tool's output directory.
     * This is useful for post-build tools that need to scan the application bytecode (for example, offline code-coverage
     * tools).
     * <p>
     * For example, if using Maven, enabling this feature will result in the classes in {@code target/classes} being
     * replaced with classes that have been transformed by Quarkus.
     * <p>
     * Setting this to {@code true}, however, should be done with a lot of caution and only if subsequent builds are done
     * in a clean environment (i.e. the build tool's output directory has been completely cleaned).
     */
    @WithDefault("false")
    boolean writeTransformedBytecodeToBuildOutput();

    /**
     * The suffix that is applied to the runner artifact's base file name.
     */
    @WithDefault("-runner")
    String runnerSuffix();

    /**
     * {@return the runner suffix if <code>addRunnerSuffix</code> is <code>true<code>, or an empty string otherwise}
     */
    default String computedRunnerSuffix() {
        return jar().addRunnerSuffix() ? runnerSuffix() : "";
    }

    /**
     * Configuration for creating packages as JARs.
     */
    @ConfigGroup
    interface JarConfig {
        /**
         * If set to false, no JAR will be produced.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * The JAR output type to use.
         */
        @WithDefault("fast-jar")
        JarType type();

        /**
         * Whether the created jar will be compressed. This setting is not used when building a native image
         */
        @ConfigDocDefault("true")
        @WithDefault("true")
        boolean compress();

        /**
         * The JAR's manifest sub-configuration.
         */
        ManifestConfig manifest();

        /**
         * Files that should not be copied to the output artifact.
         */
        Optional<List<String>> userConfiguredIgnoredEntries();

        /**
         * List of all the dependencies that have been defined as optional to include into the final package of the application.
         * Each optional dependency needs to be expressed in the following format:
         * <p>
         * {@code groupId:artifactId[:[classifier][:[type]]]}
         * <p>
         * With the classifier and type being optional (note that the brackets ({@code []}) denote optionality and are
         * not a part of the syntax specification).
         * The group ID and artifact ID must be present and non-empty.
         * <p>
         * If the type is missing, the artifact is assumed to be of type {@code jar}.
         * <p>
         * This parameter is optional; if absent, no optional dependencies will be included into the final package of
         * the application.
         * <p>
         * For backward compatibility reasons, this parameter is ignored by default and can be enabled by setting the
         * parameter {@code quarkus.package.jar.filter-optional-dependencies} to {@code true}.
         * <p>
         * This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final
         * package with unused dependencies.
         */
        Optional<Set<GACT>> includedOptionalDependencies();

        /**
         * Flag indicating whether the optional dependencies should be filtered out or not.
         * <p>
         * This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final
         * package with unused dependencies.
         */
        @WithDefault("false")
        boolean filterOptionalDependencies();

        /**
         * Indicates whether the generated JAR file should have the runner suffix appended.
         * Only applicable to the {@linkplain JarType#UBER_JAR uber-JAR output type}.
         * If disabled, the JAR built by the original build system (Maven, Gradle, etc.)
         * will be replaced with the Quarkus-built uber-JAR.
         */
        @WithDefault("true")
        boolean addRunnerSuffix();

        /**
         * AppCDS archive sub-configuration.
         * This configuration only applies to certain JAR types.
         */
        AppcdsConfig appcds();

        /**
         * Configuration for AppCDS generation.
         */
        @ConfigGroup
        interface AppcdsConfig {
            /**
             * Whether to automate the creation of AppCDS.
             * Furthermore, this option only works for Java 11+ and is considered experimental for the time being.
             * Finally, care must be taken to use the same exact JVM version when building and running the application.
             */
            @WithDefault("false")
            boolean enabled();

            /**
             * When AppCDS generation is enabled, if this property is set, then the JVM used to generate the AppCDS file
             * will be the JVM present in the container image. The builder image is expected to have the 'java' binary
             * on its PATH.
             * This flag is useful when the JVM to be used at runtime is not the same exact JVM version as the one used to build
             * the jar.
             * Note that this property is consulted only when {@code quarkus.package.jar.appcds.enabled=true} and it requires
             * having
             * docker available during the build.
             */
            Optional<String> builderImage();

            /**
             * Whether creation of the AppCDS archive should run in a container if available.
             *
             * <p>
             * Normally, if either a suitable container image to use to create the AppCDS archive
             * can be determined automatically or if one is explicitly set using the
             * {@code quarkus.<package-type>.appcds.builder-image} setting, the AppCDS archive is generated by
             * running the JDK contained in the image as a container.
             *
             * <p>
             * If this option is set to {@code false}, a container will not be used to generate the
             * AppCDS archive. Instead, the JDK used to build the application is also used to create the
             * archive. Note that the exact same JDK version must be used to run the application in this
             * case.
             *
             * <p>
             * Ignored if {@code quarkus.package.jar.appcds.enabled} is set to {@code false}.
             */
            @WithDefault("true")
            boolean useContainer();
        }

        /**
         * This is an advanced option that only takes effect for development mode.
         * <p>
         * If this is specified a directory of this name will be created in the jar distribution. Users can place
         * jar files in this directory, and when re-augmentation is performed these will be processed and added to the
         * class-path.
         * <p>
         * Note that before reaugmentation has been performed these jars will be ignored, and if they are updated the app
         * should be reaugmented again.
         */
        Optional<String> userProvidersDirectory();

        /**
         * If this option is true then a list of all the coordinates of the artifacts that made up this image will be included
         * in the quarkus-app directory. This list can be used by vulnerability scanners to determine
         * if your application has any vulnerable dependencies.
         * Only supported for the {@linkplain JarType#FAST_JAR fast JAR} and {@linkplain JarType#MUTABLE_JAR mutable JAR}
         * output types.
         */
        @WithDefault("true")
        boolean includeDependencyList();

        /**
         * Decompiler configuration
         */
        DecompilerConfig decompiler();

        /**
         * Configuration which applies to the JAR's manifest.
         */
        @ConfigGroup
        interface ManifestConfig {
            /**
             * Specify whether the `Implementation` information should be included in the runner jar's MANIFEST.MF.
             */
            @WithDefault("true")
            boolean addImplementationEntries();

            /**
             * Custom manifest attributes to be added to the main section of the MANIFEST.MF file.
             * An example of the user defined property:
             * quarkus.package.jar.manifest.attributes."Entry-key1"=Value1
             * quarkus.package.jar.manifest.attributes."Entry-key2"=Value2
             */
            @ConfigDocMapKey("attribute-name")
            Map<String, String> attributes();

            /**
             * Custom manifest sections to be added to the MANIFEST.MF file.
             * An example of the user defined property:
             * quarkus.package.jar.manifest.sections."Section-Name"."Entry-Key1"=Value1
             * quarkus.package.jar.manifest.sections."Section-Name"."Entry-Key2"=Value2
             */
            @ConfigDocMapKey("section-name")
            Map<String, Map<String, String>> sections();
        }

        /**
         * The possible packaging options for JAR output.
         */
        enum JarType {
            /**
             * The "fast JAR" packaging type.
             */
            FAST_JAR("fast-jar", "jar"),
            /**
             * The "Uber-JAR" packaging type.
             */
            UBER_JAR("uber-jar"),
            /**
             * The "mutable JAR" packaging type (for remote development mode).
             */
            MUTABLE_JAR("mutable-jar"),
            /**
             * The "legacy JAR" packaging type.
             * This corresponds to the packaging type used in Quarkus before version 1.12.
             *
             * @deprecated This packaging type is no longer recommended for use.
             */
            @Deprecated
            LEGACY_JAR("legacy-jar", "legacy"),
            ;

            public static final List<JarType> values = List.of(JarType.values());

            private static final Map<String, JarType> byName = values.stream()
                    .flatMap(item -> item.names.stream().map(name -> Map.entry(name, item)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            private final List<String> names;

            JarType(final List<String> names) {
                this.names = names;
            }

            JarType(final String... names) {
                this(List.of(names));
            }

            JarType(final String name) {
                this(List.of(name));
            }

            /**
             * {@return the names for this output type}
             * The first name is the "canonical" name.
             */
            public List<String> names() {
                return names;
            }

            /**
             * {@return the name of this output type}
             */
            @Override
            public String toString() {
                return names.get(0);
            }

            /**
             * {@return the <code>JarType</code> for the given string}
             *
             * @param value the string to look up
             * @throws IllegalArgumentException if the string does not correspond to a valid JAR type
             */
            public static JarType fromString(String value) {
                JarType jarOutputType = byName.get(value);
                if (jarOutputType == null) {
                    throw new IllegalArgumentException("Unknown JAR package type '" + value + "'");
                }
                return jarOutputType;
            }
        }
    }

    /**
     * Configuration for the decompiler.
     */
    @ConfigGroup
    interface DecompilerConfig {
        /**
         * Enable decompilation of generated and transformed bytecode into a filesystem.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The directory into which to save the decompilation output.
         * <p>
         * A relative path is understood as relative to the build directory.
         */
        @WithDefault("decompiler")
        String outputDirectory();

        /**
         * The directory into which to save the decompilation tool if it doesn't exist locally.
         */
        @WithDefault("${user.home}/.quarkus")
        String jarDirectory();
    }
}
