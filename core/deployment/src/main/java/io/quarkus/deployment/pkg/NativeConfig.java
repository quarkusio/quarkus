package io.quarkus.deployment.pkg;

import java.io.File;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class NativeConfig {

    /**
     * Additional arguments to pass to the build process
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
    @ConfigItem(defaultValue = "false")
    public boolean enableHttpsUrlHandler;

    /**
     * If all security services should be added to the native image
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableAllSecurityServices;

    /**
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated
    @ConfigItem(defaultValue = "true")
    public boolean enableJni;

    /**
     * If all character sets should be added to the native image. This increases image size
     */
    @ConfigItem(defaultValue = "false")
    public boolean addAllCharsets;

    /**
     * If all time zones should be added to the native image. This increases image size
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeAllTimeZones;

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
     * If debug symbols should be included
     */
    @ConfigItem(defaultValue = "false")
    public boolean debugSymbols;

    /**
     * If the native image build should wait for a debugger to be attached before running. This is an advanced option
     * and is generally only intended for those familiar with GraalVM internals
     */
    @ConfigItem(defaultValue = "false")
    public boolean debugBuildProcess;

    /**
     * If the debug port should be published when building with docker and debug-build-process is true
     */
    @ConfigItem(defaultValue = "true")
    public boolean publishDebugBuildProcessPort;

    /**
     * If the native image server should be restarted
     */
    @ConfigItem(defaultValue = "false")
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
    @ConfigItem(defaultValue = "false")
    public boolean enableFallbackImages;

    /**
     * If the native image server should be used. This can speed up compilation but can result in changes not always
     * being picked up due to cache invalidation not working 100%
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableServer;

    /**
     * If all META-INF/services entries should be automatically registered
     */
    @ConfigItem(defaultValue = "false")
    public boolean autoServiceLoaderRegistration;

    /**
     * If the bytecode of all proxies should be dumped for inspection
     */
    @ConfigItem(defaultValue = "false")
    public boolean dumpProxies;

    /**
     * If this build should be done using a container runtime. If this is set docker will be used by default,
     * unless container-runtime is also set.
     */
    @ConfigItem(defaultValue = "false")
    public boolean containerBuild;

    /**
     * The docker image to use to do the image build
     */
    @ConfigItem(defaultValue = "quay.io/quarkus/ubi-quarkus-native-image:19.3.1-java11")
    public String builderImage;

    /**
     * The container runtime (e.g. docker) that is used to do an image based build. If this is set then
     * a container build is always done.
     */
    @ConfigItem
    public Optional<String> containerRuntime;

    /**
     * Options to pass to the container runtime
     */
    @ConfigItem
    public Optional<List<String>> containerRuntimeOptions;

    /**
     * If the resulting image should allow VM introspection
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableVmInspection;

    /**
     * If full stack traces are enabled in the resulting image
     */
    @ConfigItem(defaultValue = "true")
    public boolean fullStackTraces;

    /**
     * If the reports on call paths and included packages/classes/methods should be generated
     */
    @ConfigItem(defaultValue = "false")
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
    @ConfigItem(defaultValue = "false")
    public boolean reportErrorsAtRuntime;

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

    }
}
