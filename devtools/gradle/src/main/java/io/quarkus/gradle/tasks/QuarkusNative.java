package io.quarkus.gradle.tasks;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImageOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImagePhase;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusNative extends QuarkusTask {

    private boolean reportErrorsAtRuntime = false;

    private boolean debugSymbols = false;

    private boolean debugBuildProcess;

    private boolean cleanupServer;

    private boolean enableHttpUrlHandler;

    private boolean enableHttpsUrlHandler;

    private boolean enableAllSecurityServices;

    private boolean enableRetainedHeapReporting;

    private boolean enableIsolates;

    private boolean enableCodeSizeReporting;

    private String graalvmHome = System.getenv("GRAALVM_HOME");

    private boolean enableServer = false;

    private boolean enableJni = false;

    private boolean autoServiceLoaderRegistration = false;

    private boolean dumpProxies = false;

    private String nativeImageXmx;

    private String containerRuntime = "docker";

    private String containerRuntimeOptions;

    private String dockerBuild;

    private boolean enableVMInspection = false;

    private boolean enableFallbackImages = false;

    private boolean fullStackTraces = true;

    private boolean disableReports;

    private List<String> additionalBuildArgs;

    private boolean addAllCharsets = false;

    private boolean reportExceptionStackTraces = true;

    public QuarkusNative() {
        super("Building a native image");
    }

    @Optional
    @Input
    public boolean isAddAllCharsets() {
        return addAllCharsets;
    }

    @Option(description = "Should all Charsets supported by the host environment be included in the native image", option = "add-all-charsets")
    public void setAddAllCharsets(final boolean addAllCharsets) {
        this.addAllCharsets = addAllCharsets;
    }

    @Optional
    @Input
    public boolean isReportErrorsAtRuntime() {
        return reportErrorsAtRuntime;
    }

    @Option(description = "Report errors at runtime", option = "report-errors-runtime")
    public void setReportErrorsAtRuntime(boolean reportErrorsAtRuntime) {
        this.reportErrorsAtRuntime = reportErrorsAtRuntime;
    }

    @Optional
    @Input
    public boolean isDebugSymbols() {
        return debugSymbols;
    }

    @Option(description = "Specify if debug symbols should be set", option = "debug-symbols")
    public void setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    @Optional
    @Input
    public boolean isDebugBuildProcess() {
        return debugBuildProcess;
    }

    @Option(description = "Specify if debug is set during build process", option = "debug-build-process")
    public void setDebugBuildProcess(boolean debugBuildProcess) {
        this.debugBuildProcess = debugBuildProcess;
    }

    @Optional
    @Input
    public boolean isCleanupServer() {
        return cleanupServer;
    }

    @Option(description = "Cleanup server", option = "cleanup-server")
    public void setCleanupServer(boolean cleanupServer) {
        this.cleanupServer = cleanupServer;
    }

    @Optional
    @Input
    public boolean isEnableHttpUrlHandler() {
        return enableHttpUrlHandler;
    }

    @Optional
    @Input
    private boolean isEnableFallbackImages() {
        return enableFallbackImages;
    }

    @Option(description = "Enable the GraalVM native image compiler to generate Fallback Images in case of compilation error. "
            +
            "Careful: these are not as efficient as normal native images.", option = "enable-fallback-images")
    public void setEnableFallbackImages(boolean enableFallbackImages) {
        this.enableFallbackImages = enableFallbackImages;
    }

    @Option(description = "Specify if http url handler is enabled", option = "enable-http-url-handler")
    public void setEnableHttpUrlHandler(boolean enableHttpUrlHandler) {
        this.enableHttpUrlHandler = enableHttpUrlHandler;
    }

    @Optional
    @Input
    public boolean isEnableHttpsUrlHandler() {
        return enableHttpsUrlHandler;
    }

    @Option(description = "Specify if https url handler is enabled", option = "enable-https-url-handler")
    public void setEnableHttpsUrlHandler(boolean enableHttpsUrlHandler) {
        this.enableHttpsUrlHandler = enableHttpsUrlHandler;
    }

    @Optional
    @Input
    public boolean isEnableAllSecurityServices() {
        return enableAllSecurityServices;
    }

    @Option(description = "Enable all security services", option = "enable-all-security-services")
    public void setEnableAllSecurityServices(boolean enableAllSecurityServices) {
        this.enableAllSecurityServices = enableAllSecurityServices;
    }

    @Optional
    @Input
    public boolean isEnableRetainedHeapReporting() {
        return enableRetainedHeapReporting;
    }

    @Option(description = "Specify if retained heap reporting should be enabled", option = "enable-retained-heap-reporting")
    public void setEnableRetainedHeapReporting(boolean enableRetainedHeapReporting) {
        this.enableRetainedHeapReporting = enableRetainedHeapReporting;
    }

    @Optional
    @Input
    public boolean isEnableIsolates() {
        return enableIsolates;
    }

    @Option(description = "Report errors at runtime", option = "enable-isolates")
    public void setEnableIsolates(boolean enableIsolates) {
        this.enableIsolates = enableIsolates;
    }

    @Optional
    @Input
    public boolean isEnableCodeSizeReporting() {
        return enableCodeSizeReporting;
    }

    @Option(description = "Report errors at runtime", option = "enable-code-size-reporting")
    public void setEnableCodeSizeReporting(boolean enableCodeSizeReporting) {
        this.enableCodeSizeReporting = enableCodeSizeReporting;
    }

    @Optional
    @Input
    public String getGraalvmHome() {
        return graalvmHome;
    }

    @Option(description = "Specify the GraalVM directory (default to $GRAALVM_HOME)", option = "graalvm")
    public void setGraalvmHome(String graalvmHome) {
        this.graalvmHome = graalvmHome;
    }

    @Optional
    @Input
    public boolean isEnableServer() {
        return enableServer;
    }

    @Option(description = "Enable server", option = "enable-server")
    public void setEnableServer(boolean enableServer) {
        this.enableServer = enableServer;
    }

    @Optional
    @Input
    public boolean isEnableJni() {
        return enableJni;
    }

    @Option(description = "Enable jni", option = "enable-jni")
    public void setEnableJni(boolean enableJni) {
        this.enableJni = enableJni;
    }

    @Optional
    @Input
    public boolean isAutoServiceLoaderRegistration() {
        return autoServiceLoaderRegistration;
    }

    @Option(description = "Auto ServiceLoader registration", option = "auto-serviceloader-registration")
    public void setAutoServiceLoaderRegistration(boolean autoServiceLoaderRegistration) {
        this.autoServiceLoaderRegistration = autoServiceLoaderRegistration;
    }

    @Optional
    @Input
    public boolean isDumpProxies() {
        return dumpProxies;
    }

    @Option(description = "Dump proxies", option = "dump-proxies")
    public void setDumpProxies(boolean dumpProxies) {
        this.dumpProxies = dumpProxies;
    }

    @Optional
    @Input
    public String getNativeImageXmx() {
        return nativeImageXmx;
    }

    @Option(description = "Specify the native image maximum heap size", option = "native-image-xmx")
    public void setNativeImageXmx(String nativeImageXmx) {
        this.nativeImageXmx = nativeImageXmx;
    }

    @Optional
    @Input
    public String getContainerRuntime() {
        return containerRuntime;
    }

    @Optional
    @Input
    public String getContainerRuntimeOptions() {
        return containerRuntimeOptions;
    }

    @Optional
    @Input
    public String getDockerBuild() {
        return dockerBuild;
    }

    @Option(description = "Container runtime", option = "container-runtime")
    @Optional
    public void setContainerRuntime(String containerRuntime) {
        this.containerRuntime = containerRuntime;
    }

    @Option(description = "Container runtime options", option = "container-runtime-options")
    @Optional
    public void setContainerRuntimeOptions(String containerRuntimeOptions) {
        this.containerRuntimeOptions = containerRuntimeOptions;
    }

    @Option(description = "Docker build", option = "docker-build")
    public void setDockerBuild(String dockerBuild) {
        this.dockerBuild = dockerBuild;
    }

    @Optional
    @Input
    public boolean isEnableVMInspection() {
        return enableVMInspection;
    }

    @Option(description = "Enable VM inspection", option = "enable-vm-inspection")
    public void setEnableVMInspection(boolean enableVMInspection) {
        this.enableVMInspection = enableVMInspection;
    }

    @Optional
    @Input
    public boolean isFullStackTraces() {
        return fullStackTraces;
    }

    @Option(description = "Specify full stacktraces", option = "full-stacktraces")
    public void setFullStackTraces(boolean fullStackTraces) {
        this.fullStackTraces = fullStackTraces;
    }

    @Optional
    @Input
    public boolean isDisableReports() {
        return disableReports;
    }

    @Option(description = "Disable reports", option = "disable-reports")
    public void setDisableReports(boolean disableReports) {
        this.disableReports = disableReports;
    }

    @Optional
    @Input
    public List<String> getAdditionalBuildArgs() {
        return additionalBuildArgs;
    }

    @Option(description = "Additional build arguments", option = "additional-build-args")
    public void setAdditionalBuildArgs(List<String> additionalBuildArgs) {
        this.additionalBuildArgs = additionalBuildArgs;
    }

    @Optional
    @Input
    public boolean isReportExceptionStackTraces() {
        return reportExceptionStackTraces;
    }

    @Option(description = "Show exception stack traces for exceptions during image building", option = "report-exception-stack-traces")
    public void setReportExceptionStackTraces(boolean reportExceptionStackTraces) {
        this.reportExceptionStackTraces = reportExceptionStackTraces;
    }

    @TaskAction
    public void buildNative() {
        getLogger().lifecycle("building native image");
        try (AppCreator appCreator = AppCreator.builder()
                // configure the build phase we want the app to go through
                .addPhase(new NativeImagePhase()
                        .setAddAllCharsets(addAllCharsets)
                        .setAdditionalBuildArgs(getAdditionalBuildArgs())
                        .setAutoServiceLoaderRegistration(isAutoServiceLoaderRegistration())
                        .setOutputDir(getProject().getBuildDir().toPath())
                        .setCleanupServer(isCleanupServer())
                        .setDebugBuildProcess(isDebugBuildProcess())
                        .setDebugSymbols(isDebugSymbols())
                        .setDisableReports(isDisableReports())
                        .setContainerRuntime(getContainerRuntime())
                        .setContainerRuntimeOptions(getContainerRuntimeOptions())
                        .setDockerBuild(getDockerBuild())
                        .setDumpProxies(isDumpProxies())
                        .setEnableAllSecurityServices(isEnableAllSecurityServices())
                        .setEnableCodeSizeReporting(isEnableCodeSizeReporting())
                        .setEnableHttpsUrlHandler(isEnableHttpsUrlHandler())
                        .setEnableHttpUrlHandler(isEnableHttpUrlHandler())
                        .setEnableIsolates(isEnableIsolates())
                        .setEnableJni(isEnableJni())
                        .setEnableRetainedHeapReporting(isEnableRetainedHeapReporting())
                        .setEnableServer(isEnableServer())
                        .setEnableVMInspection(isEnableVMInspection())
                        .setEnableFallbackImages(isEnableFallbackImages())
                        .setFullStackTraces(isFullStackTraces())
                        .setGraalvmHome(getGraalvmHome())
                        .setNativeImageXmx(getNativeImageXmx())
                        .setReportErrorsAtRuntime(isReportErrorsAtRuntime())
                        .setReportExceptionStackTraces(isReportExceptionStackTraces()))

                .build()) {

            appCreator.pushOutcome(AugmentOutcome.class, new AugmentOutcome() {
                final Path classesDir = extension().outputDirectory().toPath();

                @Override
                public Path getAppClassesDir() {
                    return classesDir;
                }

                @Override
                public Path getTransformedClassesDir() {
                    // not relevant for this mojo
                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getWiringClassesDir() {
                    // not relevant for this mojo
                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getConfigDir() {
                    return extension().outputConfigDirectory().toPath();
                }

                @Override
                public Map<Path, Set<String>> getTransformedClassesByJar() {
                    return Collections.emptyMap();
                }
            }).pushOutcome(RunnerJarOutcome.class, new RunnerJarOutcome() {
                final Path runnerJar = getProject().getBuildDir().toPath().resolve(extension().finalName() + "-runner.jar");
                final Path originalJar = getProject().getBuildDir().toPath().resolve(extension().finalName() + ".jar");

                @Override
                public Path getRunnerJar() {
                    return runnerJar;
                }

                @Override
                public Path getLibDir() {
                    return runnerJar.getParent().resolve("lib");
                }

                @Override
                public Path getOriginalJar() {
                    return originalJar;
                }
            }).resolveOutcome(NativeImageOutcome.class);

        } catch (AppCreatorException e) {
            throw new GradleException("Failed to generate a native image", e);
        }

    }
}
