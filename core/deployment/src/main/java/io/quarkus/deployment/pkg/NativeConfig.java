package io.quarkus.deployment.pkg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class NativeConfig {

    /**
     * If errors should be reported at runtime. This is a more relaxed setting, however it is not recommended as it means
     * your application may fail at runtime if an unsupported feature is used by accident
     */
    @ConfigItem(defaultValue = "false")
    public boolean reportErrorsAtRuntime;

    /**
     * If debug symbols should be included
     */
    @ConfigItem(defaultValue = "false")
    public boolean debugSymbols;

    /**
     * If the native image build should wait for a debugger to be attached before running. This is an advanced option
     * and is generally only intended for those familiar with Substrate internals
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
     * If the HTTP url handler should be enabled, allowing you to do URL.openConnection() for HTTP URLs
     */
    @ConfigItem(defaultValue = "false")
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
     * This will report on the size of the retained heap after image build
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableRetainedHeapReporting;

    /**
     * This enables reporting of the code size of the native image
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableCodeSizeReporting;

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
     * The location of the Graal distribution
     */
    @ConfigItem(defaultValue = "${GRAALVM_HOME:}")
    public String graalvmHome;

    /**
     * The location of the JDK
     */
    @ConfigItem(defaultValue = "${java.home}")
    public File javaHome;

    /**
     * If the native image server should be used. This can speed up compilation but can result in changes not always
     * being picked up due to cache invalidation not working 100%
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableServer;

    /**
     * If JNI should be enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableJni;

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
     * The default maximum old generation size of the native image
     */
    @ConfigItem
    public Optional<String> nativeImageXmx;

    /**
     * The docker image to use to do the image build
     */
    @ConfigItem(defaultValue = "quay.io/quarkus/ubi-quarkus-native-image:19.2.0")
    public String builderImage;

    /**
     * The container runtime (e.g. docker) that is used to do an image based build
     */
    @ConfigItem
    public String containerRuntime = "";

    /**
     * Options to pass to the container runtime
     */
    @ConfigItem
    public List<String> containerRuntimeOptions = new ArrayList<>();

    /**
     * If the resulting image should allow VM introspection
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableVmInspection;

    /**
     * If full stack traces are enabled in the resulting image
     */
    @ConfigItem(defaultValue = "false")
    public boolean fullStackTraces;

    /**
     * If reporting on call paths should be enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableReports;

    /**
     * Additional arguments to pass to the build process
     */
    @ConfigItem
    public List<String> additionalBuildArgs;

    /**
     * If all character sets should be added to the native image. This increases image size
     */
    @ConfigItem(defaultValue = "false")
    public boolean addAllCharsets;

    /**
     * If exceptions should be reported with a full stack trace
     */
    @ConfigItem(defaultValue = "true")
    public boolean reportExceptionStackTraces;
}
