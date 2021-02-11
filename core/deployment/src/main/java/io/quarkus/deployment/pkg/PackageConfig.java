package io.quarkus.deployment.pkg;

import java.util.List;
import java.util.Optional;

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

    /**
     * The requested output type.
     *
     * The default built in types are 'jar' (which will use 'fast-jar'), 'legacy-jar' for the pre-1.12 default jar
     * packaging, 'uber-jar' and 'native'.
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
     *
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
     *
     * If this is specified a directory of this name will be created in the jar distribution. Users can place
     * jar files in this directory, and when re-augmentation is performed these will be processed and added to the
     * class-path.
     *
     * Note that before reaugmentation has been performed these jars will be ignored, and if they are updated the app
     * should be reaugmented again.
     */
    @ConfigItem
    public Optional<String> userProvidersDirectory;

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
}
