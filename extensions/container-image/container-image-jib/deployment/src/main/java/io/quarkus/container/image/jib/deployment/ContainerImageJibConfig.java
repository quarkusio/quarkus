package io.quarkus.container.image.jib.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "jib", phase = ConfigPhase.BUILD_TIME)
public class ContainerImageJibConfig {

    public static final String DEFAULT_WORKING_DIR = "/home/jboss";
    /**
     * The base image to be used when a container image is being produced for the jar build.
     *
     * When the application is built against Java 21 or higher, {@code registry.access.redhat.com/ubi8/openjdk-21-runtime:1.18}
     * is used as the default.
     * Otherwise {@code registry.access.redhat.com/ubi8/openjdk-17-runtime:1.18} is used as the default.
     */
    @ConfigItem
    public Optional<String> baseJvmImage;

    /**
     * The base image to be used when a container image is being produced for the native binary build.
     * The default is "quay.io/quarkus/quarkus-micro-image". You can also use
     * "registry.access.redhat.com/ubi8/ubi-minimal" which is a bigger base image, but provide more built-in utilities
     * such as the microdnf package manager.
     */
    @ConfigItem(defaultValue = "quay.io/quarkus/quarkus-micro-image:2.0")
    public String baseNativeImage;

    /**
     * The JVM arguments to pass to the JVM when starting the application
     */
    @ConfigItem(defaultValue = "-Djava.util.logging.manager=org.jboss.logmanager.LogManager")
    public List<String> jvmArguments;

    /**
     * Additional JVM arguments to pass to the JVM when starting the application
     */
    @ConfigItem
    public Optional<List<String>> jvmAdditionalArguments;

    /**
     * Additional arguments to pass when starting the native application
     */
    @ConfigItem
    public Optional<List<String>> nativeArguments;

    /**
     * If this is set, then it will be used as the entry point of the container image.
     * There are a few things to be aware of when creating an entry point
     * <ul>
     * <li>Entrypoint "INHERIT" means to inherit entrypoint from base image, {@code jvmArguments} field is used for
     * arguments</li>
     * <li>A valid entrypoint is jar package specific (see {@code quarkus.package.type})</li>
     * <li>A valid entrypoint depends on the location of both the launching scripts and the application jar file. To that
     * end it's helpful to remember that when {@code fast-jar} packaging is used (the default), all necessary application
     * jars are added to the {@code /work} directory and that the same
     * directory is also used as the working directory. When {@code legacy-jar} or {@code uber-jar} are used, the application
     * jars
     * are unpacked under the {@code /app} directory
     * and that directory is used as the working directory.</li>
     * <li>Even if the {@code jvmArguments} field is set, it is ignored completely unless entrypoint is "INHERIT"</li>
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
     * <li>Entrypoint "INHERIT" means to inherit entrypoint from base image, {@code nativeArguments} field is used for
     * arguments</li>
     * <li>A valid entrypoint depends on the location of both the launching scripts and the native binary file. To that end
     * it's helpful to remember that the native application is added to the {@code /work} directory and that and the same
     * directory is also used as the working directory</li>
     * <li>Even if the {@code nativeArguments} field is set, it is ignored completely unless entrypoint is "INHERIT"</li>
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
     * The working directory to use in the generated image.
     * The default value is chosen to work in accordance with the default base image.
     */
    @ConfigItem(defaultValue = DEFAULT_WORKING_DIR)
    public String workingDirectory;

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
     * List of target platforms. Each platform is defined using the pattern:
     *
     * <pre>
     * &lt;os>|&lt;arch>[/variant]|&lt;os>/&lt;arch>[/variant]
     * </pre>
     *
     * for example:
     *
     * <pre>
     * linux/amd64,linux/arm64/v8
     * </pre>
     *
     * If not specified, OS default is linux and architecture default is {@code amd64}.
     *
     * If more than one platform is configured, it is important to note that the base image has to be a Docker manifest or an
     * OCI image index containing a version of each chosen platform.
     *
     * The feature does not work with native images, as cross-compilation is not supported.
     *
     * This configuration is based on an incubating feature of Jib. See <a href=
     * "https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#how-do-i-specify-a-platform-in-the-manifest-list-or-oci-index-of-a-base-image">Jib
     * FAQ</a> for more information.
     */
    @ConfigItem
    public Optional<Set<String>> platforms;

    /**
     * The path of a file in which the digest of the generated image will be written.
     * If the path is relative, the base path is the output directory of the build tool.
     */
    @ConfigItem(defaultValue = "jib-image.digest")
    public String imageDigestFile;

    /**
     * The path of a file in which the id of the generated image will be written.
     * If the path is relative, the base path is the output directory of the build tool.
     */
    @ConfigItem(defaultValue = "jib-image.id")
    public String imageIdFile;

    /**
     * Whether or not to operate offline.
     */
    @ConfigItem(defaultValue = "false")
    public boolean offlineMode;

    /**
     * Name of binary used to execute the docker commands. This is only used by Jib
     * when the container image is being built locally.
     */
    @ConfigItem
    public Optional<String> dockerExecutableName;

    /**
     * Sets environment variables used by the Docker executable. This is only used by Jib
     * when the container image is being built locally.
     */
    @ConfigItem
    public Map<String, String> dockerEnvironment;

    /**
     * Whether to set the creation time to the actual build time. Otherwise, the creation time
     * will be set to the Unix epoch (00:00:00, January 1st, 1970 in UTC). See <a href=
     * "https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago">Jib
     * FAQ</a> for more information
     */
    @ConfigItem(defaultValue = "true")
    public boolean useCurrentTimestamp;

    /**
     * Whether to set the modification time (last modified time) of the files put by Jib in the image to the actual
     * build time. Otherwise, the modification time will be set to the Unix epoch (00:00:00, January 1st, 1970 in UTC).
     *
     * If the modification time is constant (flag is set to false so Unix epoch is used) across two consecutive builds,
     * the docker layer sha256 digest will be different only if the actual files added by Jib to the docker layer were
     * changed. More exactly, having 2 consecutive builds will generate different docker layers only if the actual
     * content of the files within the docker layer was changed.
     *
     * If the current timestamp is used the sha256 digest of the docker layer will always be different even if the
     * content of the files didn't change.
     */
    @ConfigItem(defaultValue = "true")
    public boolean useCurrentTimestampFileModification;

    /**
     * The directory to use for caching base image layers.
     * If not specified, the Jib default directory is used.
     */
    @ConfigItem
    public Optional<String> baseImageLayersCache;

    /**
     * The directory to use for caching application layers.
     * If not specified, the Jib default directory is used.
     */
    public Optional<String> applicationLayersCache;

}
