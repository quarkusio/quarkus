package io.quarkus.deployment.pkg;

import java.io.File;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class NativeConfig {

    /**
     * Comma-separated, additional arguments to pass to the build process.
     * If an argument includes the {@code ,} symbol, it needs to be escaped, e.g. {@code \\,}
     */
    @ConfigItem
    public Optional<List<String>> additionalBuildArgs;

    /**
     * If the HTTP url handler should be enabled, allowing you to do URL.openConnection() for HTTP URLs
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableHttpUrlHandler;

    /**
     * If the HTTPS url handler should be enabled, allowing you to do URL.openConnection() for HTTPS URLs
     */
    @ConfigItem
    public boolean enableHttpsUrlHandler;

    /**
     * If all security services should be added to the native image
     */
    @ConfigItem
    public boolean enableAllSecurityServices;

    /**
     * If {@code -H:+InlineBeforeAnalysis} flag will be added to the native-image run
     */
    @ConfigItem(defaultValue = "true")
    public boolean inlineBeforeAnalysis;

    /**
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated
    @ConfigItem(defaultValue = "true")
    public boolean enableJni;

    /**
     * Defines the user language used for building the native executable.
     * <p>
     * Defaults to the system one.
     */
    @ConfigItem(defaultValue = "${user.language:}")
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> userLanguage;

    /**
     * Defines the user country used for building the native executable.
     * <p>
     * Defaults to the system one.
     */
    @ConfigItem(defaultValue = "${user.country:}")
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> userCountry;

    /**
     * Defines the file encoding as in -Dfile.encoding=...
     *
     * Native image runtime uses the host's (i.e. build time) value of file.encoding
     * system property. We intentionally default this to UTF-8 to avoid platform specific
     * defaults to be picked up which can then result in inconsistent behavior in the
     * generated native executable.
     */
    @ConfigItem(defaultValue = "UTF-8")
    @ConvertWith(TrimmedStringConverter.class)
    public String fileEncoding;

    /**
     * If all character sets should be added to the native image. This increases image size
     */
    @ConfigItem
    public boolean addAllCharsets;

    /**
     * The location of the Graal distribution
     */
    @ConfigItem(defaultValue = "${GRAALVM_HOME:}")
    public Optional<String> graalvmHome;

    /**
     * The location of the JDK
     */
    @ConfigItem(defaultValue = "${java.home}")
    public File javaHome;

    /**
     * The maximum Java heap to be used during the native image generation
     */
    @ConfigItem
    public Optional<String> nativeImageXmx;

    /**
     * If the native image build should wait for a debugger to be attached before running. This is an advanced option
     * and is generally only intended for those familiar with GraalVM internals
     */
    @ConfigItem
    public boolean debugBuildProcess;

    /**
     * If the debug port should be published when building with docker and debug-build-process is true
     */
    @ConfigItem(defaultValue = "true")
    public boolean publishDebugBuildProcessPort;

    /**
     * If the native image server should be restarted.
     *
     * @deprecated Since GraalVM 20.2.0 the native image server has become an experimental feature and is disabled by
     *             default.
     */
    @Deprecated
    @ConfigItem
    public boolean cleanupServer;

    /**
     * If isolates should be enabled
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableIsolates;

    /**
     * If a JVM based 'fallback image' should be created if native image fails. This is not recommended, as this is
     * functionally the same as just running the application in a JVM
     */
    @ConfigItem
    public boolean enableFallbackImages;

    /**
     * If the native image server should be used. This can speed up compilation but can result in changes not always
     * being picked up due to cache invalidation not working 100%
     *
     * @deprecated This used to be the default prior to GraalVM 20.2.0 and this configuration item was used to disable
     *             it as it was not stable. Since GraalVM 20.2.0 the native image server has become an experimental
     *             feature.
     */
    @Deprecated
    @ConfigItem
    public boolean enableServer;

    /**
     * If all META-INF/services entries should be automatically registered
     */
    @ConfigItem
    public boolean autoServiceLoaderRegistration;

    /**
     * If the bytecode of all proxies should be dumped for inspection
     */
    @ConfigItem
    public boolean dumpProxies;

    /**
     * If this build should be done using a container runtime. Unless container-runtime is also set, docker will be
     * used by default. If docker is not available or is an alias to podman, podman will be used instead as the default.
     */
    @ConfigItem
    public Optional<Boolean> containerBuild;

    /**
     * If this build is done using a remote docker daemon.
     */
    @ConfigItem
    public boolean remoteContainerBuild;

    public boolean isContainerBuild() {
        return containerBuild.orElse(containerRuntime.isPresent() || remoteContainerBuild);
    }

    /**
     * The docker image to use to do the image build
     */
    @ConfigItem(defaultValue = "${platform.quarkus.native.builder-image}")
    public String builderImage;

    /**
     * The container runtime (e.g. docker) that is used to do an image based build. If this is set then
     * a container build is always done.
     */
    @ConfigItem
    public Optional<ContainerRuntime> containerRuntime;

    /**
     * Options to pass to the container runtime
     */
    @ConfigItem
    public Optional<List<String>> containerRuntimeOptions;

    /**
     * If the resulting image should allow VM introspection
     */
    @ConfigItem
    public boolean enableVmInspection;

    /**
     * If full stack traces are enabled in the resulting image
     */
    @ConfigItem(defaultValue = "true")
    public boolean fullStackTraces;

    /**
     * If the reports on call paths and included packages/classes/methods should be generated
     */
    @ConfigItem
    public boolean enableReports;

    /**
     * If exceptions should be reported with a full stack trace
     */
    @ConfigItem(defaultValue = "true")
    public boolean reportExceptionStackTraces;

    /**
     * If errors should be reported at runtime. This is a more relaxed setting, however it is not recommended as it
     * means
     * your application may fail at runtime if an unsupported feature is used by accident.
     */
    @ConfigItem
    public boolean reportErrorsAtRuntime;

    /**
     * Don't build a native image if it already exists.
     *
     * This is useful if you have already built an image and you want to use Quarkus to deploy it somewhere.
     *
     * Note that this is not able to detect if the existing image is outdated, if you have modified source
     * or config and want a new image you must not use this flag.
     */
    @ConfigItem(defaultValue = "false")
    public boolean reuseExisting;

    /**
     * Build time configuration options for resources inclusion in the native executable.
     */
    @ConfigItem
    public ResourcesConfig resources;

    @ConfigGroup
    public static class ResourcesConfig {

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
         *
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
        @ConfigItem
        public Optional<List<String>> includes;

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
         *
         * the resource {@code red.png} will be available in the native image while the resources {@code foo/green.png}
         * and {@code bar/blue.png} will not be available in the native image.
         */
        @ConfigItem
        public Optional<List<String>> excludes;
    }

    /**
     * Debugging options.
     */
    @ConfigItem
    public Debug debug;

    @ConfigGroup
    public static class Debug {
        /**
         * If debug is enabled and debug symbols are generated.
         * The symbols will be generated in a separate .debug file.
         */
        @ConfigItem
        public boolean enabled;
    }

    /**
     * Generate the report files for GraalVM Dashboard.
     */
    @ConfigItem
    public boolean enableDashboardDump;

    /**
     * Supported Container runtimes
     */
    public static enum ContainerRuntime {
        DOCKER,
        PODMAN;

        public String getExecutableName() {
            return this.name().toLowerCase();
        }
    }
}
