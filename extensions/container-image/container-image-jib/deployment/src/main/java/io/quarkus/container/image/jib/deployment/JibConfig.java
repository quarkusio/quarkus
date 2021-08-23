package io.quarkus.container.image.jib.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class JibConfig {

    /**
     * The base image to be used when a container image is being produced for the jar build
     */
    @ConfigItem(defaultValue = "fabric8/java-alpine-openjdk11-jre")
    public String baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build
     */
    @ConfigItem(defaultValue = "registry.access.redhat.com/ubi8/ubi-minimal")
    public String baseNativeImage;

    /**
     * Additional JVM arguments to pass to the JVM when starting the application
     */
    @ConfigItem(defaultValue = "-Djava.util.logging.manager=org.jboss.logmanager.LogManager")
    public List<String> jvmArguments;

    /**
     * Additional arguments to pass when starting the native application
     */
    @ConfigItem
    public Optional<List<String>> nativeArguments;

    /**
     * If this is set, then it will be used as the entry point of the container image.
     * There are a few things to be aware of when creating an entry point
     * <ul>
     * <li>A valid entrypoint is jar package specific (see {@code quarkus.package.type})</li>
     * <li>A valid entrypoint depends on the location of both the launching scripts and the application jar file. To that
     * end it's helpful to remember that when {@code fast-jar} packaging is used (the default), all necessary application
     * jars are added to the {@code /work} directory and that the same
     * directory is also used as the working directory. When {@code legacy-jar} or {@code uber-jar} are used, the application
     * jars
     * are unpacked under the {@code /app} directory
     * and that directory is used as the working directory.</li>
     * <li>Even if the {@code jvmArguments} field is set, it is ignored completely</li>
     * </ul>
     *
     * When this is not set, a proper default entrypoint will be constructed.
     *
     * As a final note, a very useful tool for inspecting container image layers that can greatly aid
     * when debugging problems with endpoints is <a href="https://github.com/wagoodman/dive">dive</a>
     */
    @ConfigItem
    public Optional<List<String>> jvmEntrypoint;

    /**
     * If this is set, then it will be used as the entry point of the container image.
     * There are a few things to be aware of when creating an entry point
     * <ul>
     * <li>A valid entrypoint depends on the location of both the launching scripts and the native binary file. To that end
     * it's helpful to remember that the native application is added to the {@code /work} directory and that and the same
     * directory is also used as the working directory</li>
     * <li>Even if the {@code nativeArguments} field is set, it is ignored completely</li>
     * </ul>
     *
     * When this is not set, a proper default entrypoint will be constructed.
     *
     * As a final note, a very useful tool for inspecting container image layers that can greatly aid
     * when debugging problems with endpoints is <a href="https://github.com/wagoodman/dive">dive</a>
     */
    @ConfigItem
    public Optional<List<String>> nativeEntrypoint;

    /**
     * Environment variables to add to the container image
     */
    @ConfigItem
    public Map<String, String> environmentVariables;

    /**
     * Custom labels to add to the generated image
     */
    @ConfigItem
    public Map<String, String> labels;

    /**
     * The username to use to authenticate with the registry used to pull the base JVM image
     */
    @ConfigItem
    public Optional<String> baseRegistryUsername;

    /**
     * The password to use to authenticate with the registry used to pull the base JVM image
     */
    @ConfigItem
    public Optional<String> baseRegistryPassword;

    /**
     * The ports to expose
     */
    @ConfigItem(defaultValue = "${quarkus.http.port:8080}")
    public List<Integer> ports;

    /**
     * The user to use in generated image
     */
    @ConfigItem
    public Optional<String> user;

    /**
     * Controls the optimization which skips downloading base image layers that exist in a target
     * registry. If the user does not set this property, then read as false.
     *
     * If {@code true}, base image layers are always pulled and cached. If
     * {@code false}, base image layers will not be pulled/cached if they already exist on the
     * target registry.
     */
    @ConfigItem(defaultValue = "false")
    public boolean alwaysCacheBaseImage;

    /**
     * List of target platforms. Each platform is defined using the pattern: \<os\>|\<arch\>[/variant]|\<os\>/\<arch\>[/variant]
     * ex: linux/amd64,linux/arm64/v8. If not specified, OS default is linux and architecture default is amd64
     *
     * If more than one platform is configured, it is important to note that the base image has to be a Docker manifest or an
     * OCI image index containing a version of each chosen platform
     *
     * It doesn't work with native images, as cross-compilation is not supported
     *
     * Target Platform is a incubating feature of Jib. See <a href=
     * "https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#how-do-i-specify-a-platform-in-the-manifest-list-or-oci-index-of-a-base-image">Jib
     * FAQ</a> for more information
     */
    @ConfigItem
    public Optional<Set<String>> platforms;

    /**
     * The path of a file that will be written containing the digest of the generated image.
     * If the path is relative, is writen to the output directory of the build tool
     */
    @ConfigItem(defaultValue = "jib-image.digest")
    public String imageDigestFile;

    /**
     * The path of a file that will be written containing the id of the generated image.
     * If the path is relative, is writen to the output directory of the build tool
     */
    @ConfigItem(defaultValue = "jib-image.id")
    public String imageIdFile;

    /**
     * Whether or not to operate offline.
     */
    @ConfigItem(defaultValue = "false")
    public boolean offlineMode;
}
