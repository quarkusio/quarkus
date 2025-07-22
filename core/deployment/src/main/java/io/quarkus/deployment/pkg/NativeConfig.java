package io.quarkus.deployment.pkg;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.deployment.images.ContainerImages;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Native executables
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.native")
public interface NativeConfig {

    /**
     * Set to enable native-image building using GraalVM.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Set to prevent the native-image process from actually building the native image.
     */
    @WithDefault("false")
    boolean sourcesOnly();

    /**
     * Comma-separated, additional arguments to pass to the build process.
     * If an argument includes the {@code ,} symbol, it needs to be escaped, e.g. {@code \\,}
     */
    Optional<List<String>> additionalBuildArgs();

    /**
     * Comma-separated, additional arguments to pass to the build process.
     * The arguments are appended to those provided through {@link #additionalBuildArgs()}, as a result they may override those
     * passed through {@link #additionalBuildArgs()}.
     * By convention, this is meant to be set on the command-line, while {@link #additionalBuildArgs()} should be preferred for
     * use in properties files.
     * If an argument includes the {@code ,} symbol, it needs to be escaped, e.g. {@code \\,}
     */
    Optional<List<String>> additionalBuildArgsAppend();

    /**
     * If the HTTP url handler should be enabled, allowing you to do URL.openConnection() for HTTP URLs
     */
    @WithDefault("true")
    boolean enableHttpUrlHandler();

    /**
     * If the HTTPS url handler should be enabled, allowing you to do URL.openConnection() for HTTPS URLs
     */
    @WithDefault("false")
    boolean enableHttpsUrlHandler();

    /**
     * If all security services should be added to the native image
     *
     * @deprecated {@code --enable-all-security-services} was removed in GraalVM 21.1 https://github.com/oracle/graal/pull/3258
     */
    @WithDefault("false")
    @Deprecated
    boolean enableAllSecurityServices();

    /**
     * If {@code -H:+InlineBeforeAnalysis} flag will be added to the native-image run
     *
     * @deprecated inlineBeforeAnalysis is always enabled starting from GraalVM 21.3.
     */
    @Deprecated
    @WithDefault("true")
    boolean inlineBeforeAnalysis();

    /**
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated
    @WithDefault("true")
    boolean enableJni();

    /**
     * The default value for java.awt.headless JVM option.
     * Switching this option affects linking of awt libraries.
     */
    @WithDefault("true")
    boolean headless();

    /**
     * Defines the user language used for building the native executable.
     * With GraalVM versions prior to GraalVM for JDK 24 it also serves as the default Locale language for the native executable
     * application runtime.
     * e.g. en or cs as defined by IETF BCP 47 language tags.
     * <p>
     *
     * @deprecated Use the global quarkus.default-locale.
     */
    @Deprecated
    Optional<@WithConverter(TrimmedStringConverter.class) String> userLanguage();

    /**
     * Defines the user country used for building the native executable.
     * With GraalVM versions prior to GraalVM for JDK 24 it also serves as the default Locale country for the native executable
     * application runtime.
     * e.g. US or FR as defined by ISO 3166-1 alpha-2 codes.
     * <p>
     *
     * @deprecated Use the global quarkus.default-locale.
     */
    @Deprecated
    Optional<@WithConverter(TrimmedStringConverter.class) String> userCountry();

    /**
     * Defines the file encoding as in {@code -Dfile.encoding=...}.
     * <p>
     * Native image runtime uses the host's (i.e. build time) value of {@code file.encoding}
     * system property. We intentionally default this to UTF-8 to avoid platform specific
     * defaults to be picked up which can then result in inconsistent behavior in the
     * generated native executable.
     */
    @WithDefault("UTF-8")
    @WithConverter(TrimmedStringConverter.class)
    String fileEncoding();

    /**
     * If all character sets should be added to the native executable.
     * <p>
     * Note that some extensions (e.g. the Oracle JDBC driver) also take this setting into account to enable support for all
     * charsets at the extension level.
     * <p>
     * This increases image size.
     */
    @WithDefault("false")
    boolean addAllCharsets();

    /**
     * The location of the Graal distribution
     */
    @WithDefault("${GRAALVM_HOME:}")
    Optional<String> graalvmHome();

    /**
     * The location of the JDK
     */
    @WithDefault("${java.home}")
    File javaHome();

    /**
     * The maximum Java heap to be used during the native image generation
     */
    Optional<String> nativeImageXmx();

    /**
     * If the native image build should wait for a debugger to be attached before running. This is an advanced option
     * and is generally only intended for those familiar with GraalVM internals
     */
    @WithDefault("false")
    boolean debugBuildProcess();

    /**
     * If the debug port should be published when building with docker and debug-build-process is true
     */
    @WithDefault("true")
    boolean publishDebugBuildProcessPort();

    /**
     * If the native image server should be restarted.
     *
     * @deprecated Since GraalVM 20.2.0 the native image server has become an experimental feature and is disabled by
     *             default.
     */
    @Deprecated
    @WithDefault("false")
    boolean cleanupServer();

    /**
     * If isolates should be enabled
     */
    @WithDefault("true")
    boolean enableIsolates();

    /**
     * If a JVM based 'fallback image' should be created if native image fails. This is not recommended, as this is
     * functionally the same as just running the application in a JVM
     */
    @WithDefault("false")
    boolean enableFallbackImages();

    /**
     * If the native image server should be used. This can speed up compilation but can result in changes not always
     * being picked up due to cache invalidation not working 100%
     *
     * @deprecated This used to be the default prior to GraalVM 20.2.0 and this configuration item was used to disable
     *             it as it was not stable. Since GraalVM 20.2.0 the native image server has become an experimental
     *             feature.
     */
    @Deprecated
    @WithDefault("false")
    boolean enableServer();

    /**
     * If all META-INF/services entries should be automatically registered
     */
    @WithDefault("false")
    boolean autoServiceLoaderRegistration();

    /**
     * If the bytecode of all proxies should be dumped for inspection
     */
    @WithDefault("false")
    boolean dumpProxies();

    /**
     * If this build should be done using a container runtime. Unless container-runtime is also set, docker will be
     * used by default. If docker is not available or is an alias to podman, podman will be used instead as the default.
     */
    Optional<Boolean> containerBuild();

    /**
     * Explicit configuration option to generate a native Position Independent Executable (PIE) for Linux.
     * If the system supports PIE generation, the default behaviour is to disable it for
     * <a href="https://www.redhat.com/en/blog/position-independent-executable-pie-performance">performance reasons</a>.
     * However, some systems can only run position-independent executables,
     * so this option enables the generation of such native executables.
     */
    Optional<Boolean> pie();

    /**
     * Generate instructions for a specific machine type. Defaults to {@code x86-64-v3} on AMD64 and {@code armv8-a} on AArch64.
     * Use {@code compatibility} for best compatibility, or {@code native} for best performance if a native executable is
     * deployed on the same machine or on a machine with the same CPU features.
     * A list of all available machine types is available by executing {@code native-image -march=list}
     */
    Optional<String> march();

    /**
     * If this build is done using a remote docker daemon.
     */
    @WithDefault("false")
    boolean remoteContainerBuild();

    default boolean isExplicitContainerBuild() {
        return containerBuild().orElse(containerRuntime().isPresent() || remoteContainerBuild());
    }

    /**
     * Configuration related to the builder image, when performing native builds in a container.
     */
    BuilderImageConfig builderImage();

    interface BuilderImageConfig {
        /**
         * The docker image to use to do the image build. It can be one of `graalvm`, `mandrel`, or the full image path, e.g.
         * {@code quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21}.
         * <p>
         * <strong>Note:</strong> Builder images are available using UBI 8 and UBI 9 base images, for example:
         * <ul>
         * <li>UBI 8: {@code quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21} (UBI 8)</li>
         * <li>UBI 9: {@code quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21} (UBI 9)</li>
         * </ul>
         * <p>
         * You need to be aware that if you use a builder image using UBI9 and you plan to build a container, you must
         * ensure that the base image used in the container is also UBI9.
         */
        @WithParentName
        @WithDefault("${platform.quarkus.native.builder-image}")
        @ConfigDocDefault("mandrel")
        String image();

        /**
         * The strategy for pulling the builder image during the build.
         * <p>
         * Defaults to 'always', which will always pull the most up-to-date image;
         * useful to keep up with fixes when a (floating) tag is updated.
         * <p>
         * Use 'missing' to only pull if there is no image locally;
         * useful on development environments where building with out-of-date images is acceptable
         * and bandwidth may be limited.
         * <p>
         * Use 'never' to fail the build if there is no image locally.
         */
        @WithDefault("always")
        ImagePullStrategy pull();

        default String getEffectiveImage() {
            final String builderImageName = this.image().toUpperCase();
            if (builderImageName.equals(BuilderImageProvider.GRAALVM.name())) {
                return ContainerImages.UBI9_GRAALVM_BUILDER;
            } else if (builderImageName.equals(BuilderImageProvider.MANDREL.name())) {
                return ContainerImages.UBI9_MANDREL_BUILDER;
            } else {
                return this.image();
            }
        }
    }

    /**
     * The container runtime (e.g. docker) that is used to do an image based build. If this is set then
     * a container build is always done.
     */
    Optional<ContainerRuntimeUtil.ContainerRuntime> containerRuntime();

    /**
     * Options to pass to the container runtime
     */
    Optional<List<String>> containerRuntimeOptions();

    /**
     * If the resulting image should allow VM introspection.
     *
     * @deprecated Use {@code quarkus.native.monitoring} instead.
     */
    @WithDefault("false")
    @Deprecated
    boolean enableVmInspection();

    /**
     * Enable monitoring various monitoring options. The value should be comma separated.
     * <ul>
     * <li><code>jfr</code> for JDK flight recorder support</li>
     * <li><code>jvmstat</code> for JVMStat support</li>
     * <li><code>heapdump</code> for heampdump support</li>
     * <li><code>jmxclient</code> for JMX client support (experimental)</li>
     * <li><code>jmxserver</code> for JMX server support (experimental)</li>
     * <li><code>nmt</code> for native memory tracking support</li>
     * <li><code>all</code> for all monitoring features</li>
     * </ul>
     */
    Optional<List<MonitoringOption>> monitoring();

    /**
     * If full stack traces are enabled in the resulting image
     *
     * @deprecated GraalVM 23.1+ will always build with full stack traces.
     */
    @WithDefault("true")
    @Deprecated
    boolean fullStackTraces();

    /**
     * If the reports on call paths and included packages/classes/methods should be generated
     */
    @WithDefault("false")
    boolean enableReports();

    /**
     * If exceptions should be reported with a full stack trace
     */
    @WithDefault("true")
    boolean reportExceptionStackTraces();

    /**
     * If errors should be reported at runtime. This is a more relaxed setting, however it is not recommended as it
     * means
     * your application may fail at runtime if an unsupported feature is used by accident.
     * <p>
     * Note that the use of this flag may result in build time failures due to {@code ClassNotFoundException}s.
     * Reason most likely being that the Quarkus extension already optimized it away or do not actually need it.
     * In such cases you should explicitly add the corresponding dependency providing the missing classes as a
     * dependency to your project.
     */
    @WithDefault("false")
    boolean reportErrorsAtRuntime();

    /**
     * Don't build a native image if it already exists.
     * <p>
     * This is useful if you have already built an image and you want to use Quarkus to deploy it somewhere.
     * <p>
     * Note that this is not able to detect if the existing image is outdated, if you have modified source
     * or config and want a new image you must not use this flag.
     */
    @WithDefault("false")
    boolean reuseExisting();

    /**
     * Build time configuration options for resources inclusion in the native executable.
     */
    ResourcesConfig resources();

    interface ResourcesConfig {

        /**
         * A comma separated list of globs to match resource paths that should be added to the native image.
         * <p>
         * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash.
         * <p>
         * By default, no resources are included.
         * <p>
         * Example: Given that you have {@code src/main/resources/ignored.png}
         * and {@code src/main/resources/foo/selected.png} in your source tree and one of your dependency JARs contains
         * {@code bar/some.txt} file, with the following configuration
         *
         * <pre>
         * quarkus.native.resources.includes = foo/**,bar/**&#47;*.txt
         * </pre>
         * <p>
         * the files {@code src/main/resources/foo/selected.png} and {@code bar/some.txt} will be included in the native
         * image, while {@code src/main/resources/ignored.png} will not be included.
         * <p>
         * <h3>Supported glob features</h3>
         * <table>
         * <tr>
         * <th>Feature</th>
         * <th>Description</th>
         * </tr>
         * <tr>
         * <td><code>*</code></td>
         * <td>Matches a (possibly empty) sequence of characters that does not contain slash ({@code /})</td>
         * </tr>
         * <tr>
         * <td><code>**</code></td>
         * <td>Matches a (possibly empty) sequence of characters that may contain slash ({@code /})</td>
         * </tr>
         * <tr>
         * <td><code>?</code></td>
         * <td>Matches one character, but not slash</td>
         * </tr>
         * <tr>
         * <td><code>[abc]</code></td>
         * <td>Matches one character given in the bracket, but not slash</td>
         * </tr>
         * <tr>
         * <td><code>[a-z]</code></td>
         * <td>Matches one character from the range given in the bracket, but not slash</td>
         * </tr>
         * <tr>
         * <td><code>[!abc]</code></td>
         * <td>Matches one character not named in the bracket; does not match slash</td>
         * </tr>
         * <tr>
         * <td><code>[a-z]</code></td>
         * <td>Matches one character outside the range given in the bracket; does not match slash</td>
         * </tr>
         * <tr>
         * <td><code>{one,two,three}</code></td>
         * <td>Matches any of the alternating tokens separated by comma; the tokens may contain wildcards, nested
         * alternations and ranges</td>
         * </tr>
         * <tr>
         * <td><code>\</code></td>
         * <td>The escape character</td>
         * </tr>
         * </table>
         * <p>
         * Note that there are three levels of escaping when passing this option via {@code application.properties}:
         * <ol>
         * <li>{@code application.properties} parser</li>
         * <li>MicroProfile Config list converter that splits the comma separated list</li>
         * <li>Glob parser</li>
         * </ol>
         * All three levels use backslash ({@code \}) as the escaping character. So you need to use an appropriate
         * number of backslashes depending on which level you want to escape.
         * <p>
         * Note that Quarkus extensions typically include the resources they require by themselves. This option is
         * useful in situations when the built-in functionality is not sufficient.
         */
        Optional<List<String>> includes();

        /**
         * A comma separated list of globs to match resource paths that should <b>not</b> be added to the native image.
         * <p>
         * Use slash ({@code /}) as a path separator on all platforms. Globs must not start with slash.
         * <p>
         * Please refer to {@link #includes} for details about the glob syntax.
         * <p>
         * By default, no resources are excluded.
         * <p>
         * Example: Given that you have {@code src/main/resources/red.png}
         * and {@code src/main/resources/foo/green.png} in your source tree and one of your dependency JARs contains
         * {@code bar/blue.png} file, with the following configuration
         *
         * <pre>
         * quarkus.native.resources.includes = **&#47;*.png
         * quarkus.native.resources.excludes = foo/**,**&#47;green.png
         * </pre>
         * <p>
         * the resource {@code red.png} will be available in the native image while the resources {@code foo/green.png}
         * and {@code bar/blue.png} will not be available in the native image.
         */
        Optional<List<String>> excludes();
    }

    /**
     * Debugging options.
     */
    Debug debug();

    @ConfigGroup
    interface Debug {
        /**
         * If debug is enabled and debug symbols are generated.
         * The symbols will be generated in a separate .debug file.
         */
        @WithDefault("false")
        boolean enabled();
    }

    /**
     * Generate the report files for GraalVM Dashboard.
     */
    @WithDefault("false")
    boolean enableDashboardDump();

    /**
     * Include a reasons entries in the generated json configuration files.
     */
    @WithDefault("false")
    boolean includeReasonsInConfigFiles();

    /**
     * Configure native executable compression using UPX.
     */
    Compression compression();

    /**
     * Configuration files generated by the Quarkus build, using native image agent, are informative by default.
     * In other words, the generated configuration files are presented in the build log but are not applied.
     * When this option is set to true, generated configuration files are applied to the native executable building process.
     * <p>
     * Enabling this option should be done with care, because it can make native image configuration and/or behaviour
     * dependant on other non-obvious factors. For example, if the native image agent generated configuration was generated
     * from running JVM unit tests, disabling test(s) can result in a different native image configuration being generated,
     * which in turn can misconfigure the native executable or affect its behaviour in unintended ways.
     */
    @WithDefault("false")
    boolean agentConfigurationApply();

    @ConfigGroup
    interface Compression {
        /**
         * Whether compression should be enabled.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Whether the compression should be executed within a container.
         */
        Optional<Boolean> containerBuild();

        /**
         * The image used for compression. Defaults to {@code quarkus.native.builder-image} if not
         * set.
         * <p>
         * Setting this variable will automatically activate
         */
        Optional<String> containerImage();

        /**
         * The compression level in [1, 10].
         * 10 means <em>best</em>.
         * <p>
         * Higher compression level requires more time to compress the executable.
         */
        OptionalInt level();

        /**
         * Allows passing extra arguments to the UPX command line (like --brute).
         * The arguments are comma-separated.
         * <p>
         * The exhaustive list of parameters can be found in
         * <a href="https://github.com/upx/upx/blob/devel/doc/upx.pod">https://github.com/upx/upx/blob/devel/doc/upx.pod</a>.
         */
        Optional<List<String>> additionalArgs();
    }

    /**
     * Supported Builder Image providers/distributions
     */
    enum BuilderImageProvider {
        GRAALVM,
        MANDREL;
    }

    enum MonitoringOption {
        HEAPDUMP,
        JVMSTAT,
        JFR,
        JMXSERVER,
        JMXCLIENT,
        NMT,
        ALL
    }

    enum ImagePullStrategy {
        /**
         * Always pull the most recent image.
         */
        ALWAYS,
        /**
         * Only pull the image if it's missing locally.
         */
        MISSING,
        /**
         * Never pull any image; fail if the image is missing locally.
         */
        NEVER
    }
}
