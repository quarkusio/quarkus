package io.quarkus.deployment.pkg;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class PackageConfig {

    public static final String JAR = "jar";
    public static final String NATIVE = "native";

    /**
     * The requested output type.
     * 
     * The default built in types are jar and native
     */
    @ConfigItem(defaultValue = JAR)
    public String type;

    /**
     * If the java runner should be packed as an uberjar
     */
    @ConfigItem
    public boolean uberJar;

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
}
