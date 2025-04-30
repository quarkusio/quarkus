package io.quarkus.container.image.jib.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.deployment.images.ContainerImages;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.jib")
public interface ContainerImageJibConfig {

    String DEFAULT_WORKING_DIR = "/home/jboss";

    /**
     * The base image to be used when a container image is being produced for the jar build.
     *
     * When the application is built against Java 21 or higher, {@code registry.access.redhat.com/ubi9/openjdk-21-runtime:1.21}
     * is used as the default.
     * Otherwise {@code registry.access.redhat.com/ubi9/openjdk-17-runtime:1.21} is used as the default.
     */
    Optional<String> baseJvmImage();

    /**
     * The base image to be used when a container image is being produced for the native binary build.
     * The default is "quay.io/quarkus/ubi9-quarkus-micro-image:2.0". You can also use
     * "registry.access.redhat.com/ubi9/ubi-minimal" which is a bigger base image, but provide more built-in utilities
     * such as the microdnf package manager.
     */
    @WithDefault(ContainerImages.QUARKUS_MICRO_IMAGE)
    String baseNativeImage();

    /**
     * The JVM arguments to pass to the JVM when starting the application
     */
    @WithDefault("-Djava.util.logging.manager=org.jboss.logmanager.LogManager")
    List<String> jvmArguments();

    /**
     * Additional JVM arguments to pass to the JVM when starting the application
     */
    Optional<List<String>> jvmAdditionalArguments();

    /**
     * Additional arguments to pass when starting the native application
     */
    Optional<List<String>> nativeArguments();

    /**
     * If this is set, then it will be used as the entry point of the container image.
     * There are a few things to be aware of when creating an entry point
     * <ul>
     * <li>Entrypoint "INHERIT" means to inherit entrypoint from base image, {@code jvmArguments} field is used for
     * arguments</li>
     * <li>A valid entrypoint is jar package specific (see {@code quarkus.package.jar.type})</li>
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
    Optional<List<String>> jvmEntrypoint();

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
    Optional<List<String>> nativeEntrypoint();

    /**
     * Environment variables to add to the container image
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> environmentVariables();

    /**
     * The username to use to authenticate with the registry used to pull the base JVM image
     */
    Optional<String> baseRegistryUsername();

    /**
     * The password to use to authenticate with the registry used to pull the base JVM image
     */
    Optional<String> baseRegistryPassword();

    /**
     * The ports to expose
     */
    @WithDefault("${quarkus.http.port:8080}")
    List<Integer> ports();

    /**
     * The user to use in generated image
     */
    Optional<String> user();

    /**
     * The working directory to use in the generated image.
     * The default value is chosen to work in accordance with the default base image.
     */
    @WithDefault(DEFAULT_WORKING_DIR)
    String workingDirectory();

    /**
     * Controls the optimization which skips downloading base image layers that exist in a target
     * registry. If the user does not set this property, then read as false.
     *
     * If {@code true}, base image layers are always pulled and cached. If
     * {@code false}, base image layers will not be pulled/cached if they already exist on the
     * target registry.
     */
    @WithDefault("false")
    boolean alwaysCacheBaseImage();

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
    Optional<Set<String>> platforms();

    /**
     * The path of a file in which the digest of the generated image will be written.
     * If the path is relative, the base path is the output directory of the build tool.
     */
    @WithDefault("jib-image.digest")
    String imageDigestFile();

    /**
     * The path of a file in which the id of the generated image will be written.
     * If the path is relative, the base path is the output directory of the build tool.
     */
    @WithDefault("jib-image.id")
    String imageIdFile();

    /**
     * Whether, or not to operate offline.
     */
    @WithDefault("false")
    boolean offlineMode();

    /**
     * Name of binary used to execute the docker commands. This is only used by Jib
     * when the container image is being built locally.
     */
    Optional<String> dockerExecutableName();

    /**
     * Sets environment variables used by the Docker executable. This is only used by Jib
     * when the container image is being built locally.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> dockerEnvironment();

    /**
     * Whether to set the creation time to the actual build time. Otherwise, the creation time
     * will be set to the Unix epoch (00:00:00, January 1st, 1970 in UTC). See <a href=
     * "https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago">Jib
     * FAQ</a> for more information
     */
    @WithDefault("true")
    boolean useCurrentTimestamp();

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
    @WithDefault("true")
    boolean useCurrentTimestampFileModification();

    /**
     * The directory to use for caching base image layers.
     * If not specified, the Jib default directory is used.
     */
    Optional<String> baseImageLayersCache();

    /**
     * The directory to use for caching application layers.
     * If not specified, the Jib default directory is used.
     */
    Optional<String> applicationLayersCache();

}
