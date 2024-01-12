package io.quarkus.deployment.pkg;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class PackageConfig {

    /**
     * @deprecated Use {@link BuiltInType#JAR} instead
     */
    @Deprecated
    public static final String JAR = BuiltInType.JAR.value;
    /**
     * @deprecated Use {@link BuiltInType#UBER_JAR} instead
     */
    @Deprecated
    public static final String UBER_JAR = BuiltInType.UBER_JAR.value;
    /**
     * @deprecated Use {@link BuiltInType#FAST_JAR} instead
     */
    @Deprecated
    public static final String FAST_JAR = BuiltInType.FAST_JAR.value;
    /**
     * @deprecated Use {@link BuiltInType#MUTABLE_JAR} instead
     */
    @Deprecated
    public static final String MUTABLE_JAR = BuiltInType.MUTABLE_JAR.value;
    /**
     * @deprecated use 'legacy-jar' instead
     */
    @Deprecated
    public static final String LEGACY = BuiltInType.LEGACY.value;
    /**
     * @deprecated Use {@link BuiltInType#LEGACY_JAR} instead
     */
    @Deprecated
    public static final String LEGACY_JAR = BuiltInType.LEGACY_JAR.value;
    /**
     * @deprecated Use {@link BuiltInType#NATIVE} instead
     */
    @Deprecated
    public static final String NATIVE = BuiltInType.NATIVE.value;
    /**
     * @deprecated Use {@link BuiltInType#NATIVE_SOURCES} instead
     */
    @Deprecated
    // does everything 'native' but stops short of actually executing the 'native-image' command
    public static final String NATIVE_SOURCES = BuiltInType.NATIVE_SOURCES.value;

    public enum BuiltInType {
        JAR("jar"),
        UBER_JAR("uber-jar"),
        FAST_JAR("fast-jar"),
        MUTABLE_JAR("mutable-jar"),
        /**
         * @deprecated use {@link #LEGACY_JAR} instead
         */
        @Deprecated
        LEGACY("legacy"),
        LEGACY_JAR("legacy-jar"),
        NATIVE("native"),
        // does everything 'native' but stops short of actually executing the 'native-image' command
        NATIVE_SOURCES("native-sources");

        private final String value;

        private BuiltInType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static BuiltInType fromString(String value) {
            for (PackageConfig.BuiltInType type : values()) {
                if (type.toString().equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown Quarkus package type '" + value + "'");
        }
    }

    /**
     * The requested output type.
     * <p>
     * The default built in types are 'jar' (which will use 'fast-jar'), 'legacy-jar' for the pre-1.12 default jar
     * packaging, 'uber-jar', 'mutable-jar' (for remote development mode), 'native' and 'native-sources'.
     */
    @ConfigItem(defaultValue = "jar")
    public String type;

    /**
     * Whether the created jar will be compressed. This setting is not used when building a native image
     */
    @ConfigItem
    @ConfigDocDefault("false")
    public Optional<Boolean> compressJar;

    /**
     * Manifest configuration of the runner jar.
     */
    @ConfigItem
    public ManifestConfig manifest;

    /**
     * The entry point of the application. This can either be a fully qualified name of a standard Java
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
     * Indicates whether the generated binary file (uber-jar or native image) should have the runner suffix appended.
     * Turning off the runner suffix in case of the uber-jar package type, the original build system (Maven, Gradle, etc)
     * built JAR will be replaced with the Quarkus built uber JAR.
     */
    @ConfigItem(defaultValue = "true")
    public boolean addRunnerSuffix;

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
     * Whether to automate the creation of AppCDS. This has no effect when a native binary is needed and will be ignored in
     * that case.
     * Furthermore, this option only works for Java 11+ and is considered experimental for the time being.
     * Finally, care must be taken to use the same exact JVM version when building and running the application.
     */
    @ConfigItem
    public boolean createAppcds;

    /**
     * When AppCDS generation is enabled, if this property is set, then the JVM used to generate the AppCDS file
     * will be the JVM present in the container image. The builder image is expected to have the 'java' binary
     * on its PATH.
     * This flag is useful when the JVM to be used at runtime is not the same exact JVM version as the one used to build
     * the jar.
     * Note that this property is consulted only when {@code quarkus.package.create-appcds=true} and it requires having
     * docker available during the build.
     */
    @ConfigItem
    public Optional<String> appcdsBuilderImage;

    /**
     * Whether creation of the AppCDS archive should run in a container if available.
     *
     * <p>
     * Normally, if either a suitable container image to create the AppCDS archive inside of
     * can be determined automatically or if one is explicitly set using the
     * {@code quarkus.package.appcds-builder-image} setting, the AppCDS archive is generated by
     * running the JDK contained in the image as a container.
     *
     * <p>
     * If this option is set to {@code false}, a container will not be used to generate the
     * AppCDS archive. Instead, the JDK used to build the application is also used to create the
     * archive. Note that the exact same JDK version must be used to run the application in this
     * case.
     *
     * <p>
     * Ignored if {@code quarkus.package.create-appcds} is set to {@code false}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean appcdsUseContainer;

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
     * Vineflower Decompiler configuration
     *
     * @Deprecated use {@code quarkus.package.decompiler} instead
     */
    @ConfigItem
    @Deprecated(forRemoval = true)
    public DecompilerConfig vineflower;

    /**
     * Decompiler configuration
     */
    @ConfigItem
    public DecompilerConfig decompiler;

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
        return (type.equalsIgnoreCase(PackageConfig.BuiltInType.JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.FAST_JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.UBER_JAR.getValue())) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.LEGACY_JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.LEGACY.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.MUTABLE_JAR.getValue());
    }

    public boolean isFastJar() {
        return type.equalsIgnoreCase(PackageConfig.BuiltInType.JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.FAST_JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.MUTABLE_JAR.getValue());
    }

    public boolean isLegacyJar() {
        return (type.equalsIgnoreCase(PackageConfig.BuiltInType.LEGACY_JAR.getValue()) ||
                type.equalsIgnoreCase(PackageConfig.BuiltInType.LEGACY.getValue()));
    }

    public boolean isUberJar() {
        return type.equalsIgnoreCase(PackageConfig.BuiltInType.UBER_JAR.getValue());
    }

    public boolean isNativeOrNativeSources() {
        return type.equalsIgnoreCase(PackageConfig.BuiltInType.NATIVE.getValue())
                || type.equalsIgnoreCase(PackageConfig.BuiltInType.NATIVE_SOURCES.getValue());
    }

    public String getRunnerSuffix() {
        return addRunnerSuffix ? runnerSuffix : "";
    }

    @ConfigGroup
    public static class DecompilerConfig {
        /**
         * An advanced option that will decompile generated and transformed bytecode into the 'decompiled' directory.
         * This is only taken into account when fast-jar is used.
         */
        @ConfigItem
        @ConfigDocDefault("false")
        public Optional<Boolean> enabled;

        /**
         * The directory into which to save the Vineflower tool if it doesn't exist
         */
        @ConfigItem
        @ConfigDocDefault("${user.home}/.quarkus")
        public Optional<String> jarDirectory;
    }
}
