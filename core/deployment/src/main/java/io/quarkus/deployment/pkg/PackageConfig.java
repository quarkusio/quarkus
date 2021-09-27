package io.quarkus.deployment.pkg;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class PackageConfig {

    public static final String JAR = "jar";
    public static final String UBER_JAR = "uber-jar";
    public static final String FAST_JAR = "fast-jar";
    public static final String MUTABLE_JAR = "mutable-jar";
    /**
     * @deprecated use 'legacy-jar' instead
     */
    @Deprecated
    public static final String LEGACY = "legacy";
    public static final String LEGACY_JAR = "legacy-jar";
    public static final String NATIVE = "native";
    // does everything 'native' but stops short of actually executing the 'native-image' command
    public static final String NATIVE_SOURCES = "native-sources";

    /**
     * The requested output type.
     * <p>
     * The default built in types are 'jar' (which will use 'fast-jar'), 'legacy-jar' for the pre-1.12 default jar
     * packaging, 'uber-jar', 'native' and 'native-sources'.
     */
    @ConfigItem(defaultValue = JAR)
    public String type;

    /**
     * Manifest configuration of the runner jar.
     */
    @ConfigItem
    public ManifestConfig manifest;

    /**
     * The entry point of the application. This can either be a a fully qualified name of a standard Java
     * class with a main method, or {@link io.quarkus.runtime.QuarkusApplication}.
     * <p>
     * If your application has main classes annotated with {@link io.quarkus.runtime.annotations.QuarkusMain}
     * then this can also reference the name given in the annotation, to avoid the need to specify fully qualified
     * names in the config.
     */
    @ConfigItem
    public Optional<String> mainClass;

    /**
     * Files that should not be copied to the output artifact
     */
    @ConfigItem
    public Optional<List<String>> userConfiguredIgnoredEntries;

    /**
     * List of all the dependencies that have been defined as optional to include into the final package of the application.
     * Each optional dependency needs to be expressed in the following format:
     * <p>
     * groupId:artifactId:classifier:type
     * <p>
     * With the classifier and type being optional.
     * <p>
     * If the type is missing, the artifact is assumed to be of type {@code jar}.
     * <p>
     * This parameter is optional, if absent, no optional dependencies will be included into the final package of
     * the application.
     * <p>
     * For backward compatibility reasons, this parameter is ignored by default and can be enabled by setting the
     * parameter {@code quarkus.package.filter-optional-dependencies} to {@code true}.
     * <p>
     * This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final
     * package with unused dependencies.
     */
    @ConfigItem
    public Optional<Set<String>> includedOptionalDependencies;

    /**
     * Flag indicating whether the optional dependencies should be filtered out or not.
     * <p>
     * This parameter is meant to be used in modules where multi-builds have been configured to avoid getting a final
     * package with unused dependencies.
     */
    @ConfigItem(defaultValue = "false")
    public boolean filterOptionalDependencies;

    /**
     * The suffix that is applied to the runner jar and native images
     */
    @ConfigItem(defaultValue = "-runner")
    public String runnerSuffix;

    /**
     * The output folder in which to place the output, this is resolved relative to the build
     * systems target directory.
     */
    @ConfigItem
    public Optional<String> outputDirectory;

    /**
     * The name of the final artifact
     */
    @ConfigItem
    public Optional<String> outputName;

    /**
     * Whether to automate the creation of AppCDS. This has not effect when a native binary is needed and will be ignored in
     * that case.
     * Furthermore, this option only works for Java 11+ and is considered experimental for the time being.
     * Finally, care must be taken to use the same exact JVM version when building and running the application.
     */
    @ConfigItem
    public boolean createAppcds;

    /**
     * When AppCDS generation is enabled, if this property is set, then the JVM used to generate the AppCDS file
     * will be the JVM present in the container image. The builder image is expected to have have the 'java' binary
     * on its PATH.
     * This flag is useful when the JVM to be used at runtime is not the same exact JVM version as the one used to build
     * the jar.
     * Note that this property is consulted only when {@code quarkus.package.create-appcds=true} and it requires having
     * docker available during the build.
     */
    @ConfigItem
    public Optional<String> appcdsBuilderImage;

    /**
     * This is an advanced option that only takes effect for the mutable-jar format.
     * <p>
     * If this is specified a directory of this name will be created in the jar distribution. Users can place
     * jar files in this directory, and when re-augmentation is performed these will be processed and added to the
     * class-path.
     * <p>
     * Note that before reaugmentation has been performed these jars will be ignored, and if they are updated the app
     * should be reaugmented again.
     */
    @ConfigItem
    public Optional<String> userProvidersDirectory;

    /**
     * This option only applies when using fast-jar or mutable-jar. If this option is true
     * then a list of all the coordinates of the artifacts that made up this image will be included
     * in the quarkus-app directory. This list can be used by vulnerability scanners to determine
     * if your application has any vulnerable dependencies.
     */
    @ConfigItem(defaultValue = "true")
    public boolean includeDependencyList;

    /**
     * Fernflower Decompiler configuration
     */
    @ConfigItem
    public FernflowerConfig fernflower;

    /**
     * If set to {@code true}, it will result in the Quarkus writing the transformed application bytecode
     * to the build tool's output directory.
     * This is useful for post-build tools that need to scan the application bytecode - for example for offline
     * code-coverage tools.
     *
     * For example, if using Maven, enabling this feature will result in the classes in {@code target/classes} being
     * updated with the versions that result after Quarkus has applied its transformations.
     *
     * Setting this to {@code true} however, should be done with a lot of caution and only if subsequent builds are done
     * in a clean environment (i.e. the build tool's output directory has been completely cleaned).
     */
    @ConfigItem
    public boolean writeTransformedBytecodeToBuildOutput;

    public boolean isAnyJarType() {
        return (type.equalsIgnoreCase(PackageConfig.JAR) ||
                type.equalsIgnoreCase(PackageConfig.FAST_JAR) ||
                type.equalsIgnoreCase(PackageConfig.UBER_JAR)) ||
                type.equalsIgnoreCase(PackageConfig.LEGACY_JAR) ||
                type.equalsIgnoreCase(PackageConfig.LEGACY) ||
                type.equalsIgnoreCase(PackageConfig.MUTABLE_JAR);
    }

    public boolean isFastJar() {
        return type.equalsIgnoreCase(PackageConfig.JAR) ||
                type.equalsIgnoreCase(PackageConfig.FAST_JAR) ||
                type.equalsIgnoreCase(PackageConfig.MUTABLE_JAR);
    }

    public boolean isLegacyJar() {
        return (type.equalsIgnoreCase(PackageConfig.LEGACY_JAR) ||
                type.equalsIgnoreCase(PackageConfig.LEGACY));
    }

    public boolean isUberJar() {
        return type.equalsIgnoreCase(PackageConfig.UBER_JAR);
    }

    @ConfigGroup
    public static class FernflowerConfig {

        /**
         * An advanced option that will decompile generated and transformed bytecode into the 'decompiled' directory.
         * This is only taken into account when fast-jar is used.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * The git hash to use to download the fernflower tool from https://jitpack.io/com/github/fesh0r/fernflower/
         */
        @ConfigItem(defaultValue = "dbf407a655")
        public String hash;

        /**
         * The directory into which to save the fernflower tool if it doesn't exist
         */
        @ConfigItem(defaultValue = "${user.home}/.quarkus")
        public String jarDirectory;
    }
}
