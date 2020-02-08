package io.quarkus.gradle.tasks;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;

public class QuarkusNative extends QuarkusTask {

    private boolean reportErrorsAtRuntime = false;

    private boolean debugSymbols = false;

    private boolean debugBuildProcess;

    private boolean cleanupServer;

    private boolean enableHttpUrlHandler;

    private boolean enableHttpsUrlHandler;

    private boolean enableAllSecurityServices;

    private boolean enableIsolates;

    private String graalvmHome = System.getenv("GRAALVM_HOME");

    private boolean enableServer = false;

    /**
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Deprecated
    private boolean enableJni = true;

    private boolean autoServiceLoaderRegistration = false;

    private boolean dumpProxies = false;

    private String nativeImageXmx;

    private String containerRuntime;

    private String containerRuntimeOptions;

    private String dockerBuild;

    private String nativeBuilderImage;

    private boolean enableVMInspection = false;

    private boolean enableFallbackImages = false;

    private boolean fullStackTraces = true;

    private boolean enableReports;

    private List<String> additionalBuildArgs;

    private boolean addAllCharsets = false;

    private boolean reportExceptionStackTraces = true;

    public QuarkusNative() {
        super("Building a native image");
    }

    @Input
    public boolean isAddAllCharsets() {
        return addAllCharsets;
    }

    @Option(description = "Should all Charsets supported by the host environment be included in the native image", option = "add-all-charsets")
    public void setAddAllCharsets(final boolean addAllCharsets) {
        this.addAllCharsets = addAllCharsets;
    }

    @Input
    public boolean isReportErrorsAtRuntime() {
        return reportErrorsAtRuntime;
    }

    @Option(description = "Report errors at runtime", option = "report-errors-runtime")
    public void setReportErrorsAtRuntime(boolean reportErrorsAtRuntime) {
        this.reportErrorsAtRuntime = reportErrorsAtRuntime;
    }

    @Input
    public boolean isDebugSymbols() {
        return debugSymbols;
    }

    @Option(description = "Specify if debug symbols should be set", option = "debug-symbols")
    public void setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    @Input
    public boolean isDebugBuildProcess() {
        return debugBuildProcess;
    }

    @Option(description = "Specify if debug is set during build process", option = "debug-build-process")
    public void setDebugBuildProcess(boolean debugBuildProcess) {
        this.debugBuildProcess = debugBuildProcess;
    }

    @Input
    public boolean isCleanupServer() {
        return cleanupServer;
    }

    @Option(description = "Cleanup server", option = "cleanup-server")
    public void setCleanupServer(boolean cleanupServer) {
        this.cleanupServer = cleanupServer;
    }

    @Input
    public boolean isEnableHttpUrlHandler() {
        return enableHttpUrlHandler;
    }

    @Input
    public boolean isEnableFallbackImages() {
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

    @Input
    public boolean isEnableHttpsUrlHandler() {
        return enableHttpsUrlHandler;
    }

    @Option(description = "Specify if https url handler is enabled", option = "enable-https-url-handler")
    public void setEnableHttpsUrlHandler(boolean enableHttpsUrlHandler) {
        this.enableHttpsUrlHandler = enableHttpsUrlHandler;
    }

    @Input
    public boolean isEnableAllSecurityServices() {
        return enableAllSecurityServices;
    }

    @Option(description = "Enable all security services", option = "enable-all-security-services")
    public void setEnableAllSecurityServices(boolean enableAllSecurityServices) {
        this.enableAllSecurityServices = enableAllSecurityServices;
    }

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
    public String getGraalvmHome() {
        return graalvmHome;
    }

    @Option(description = "Specify the GraalVM directory (default to $GRAALVM_HOME)", option = "graalvm")
    public void setGraalvmHome(String graalvmHome) {
        this.graalvmHome = graalvmHome;
    }

    @Input
    public boolean isEnableServer() {
        return enableServer;
    }

    @Option(description = "Enable server", option = "enable-server")
    public void setEnableServer(boolean enableServer) {
        this.enableServer = enableServer;
    }

    @Input
    @Deprecated
    public boolean isEnableJni() {
        return enableJni;
    }

    /**
     * @param enableJni true to enable JNI
     * @deprecated JNI is always enabled starting from GraalVM 19.3.1.
     */
    @Option(description = "Enable jni (deprecated)", option = "enable-jni")
    @Deprecated
    public void setEnableJni(boolean enableJni) {
        this.enableJni = enableJni;
    }

    @Input
    public boolean isAutoServiceLoaderRegistration() {
        return autoServiceLoaderRegistration;
    }

    @Option(description = "Auto ServiceLoader registration", option = "auto-service-loader-registration")
    public void setAutoServiceLoaderRegistration(boolean autoServiceLoaderRegistration) {
        this.autoServiceLoaderRegistration = autoServiceLoaderRegistration;
    }

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
    public void setContainerRuntime(String containerRuntime) {
        this.containerRuntime = containerRuntime;
    }

    @Option(description = "Container runtime options", option = "container-runtime-options")
    public void setContainerRuntimeOptions(String containerRuntimeOptions) {
        this.containerRuntimeOptions = containerRuntimeOptions;
    }

    @Option(description = "Docker build", option = "docker-build")
    public void setDockerBuild(String dockerBuild) {
        this.dockerBuild = dockerBuild;
    }

    @Option(description = "Docker image", option = "native-builder-image")
    public void setNativeBuilderImage(String nativeBuilderImage) {
        this.nativeBuilderImage = nativeBuilderImage;
    }

    @Optional
    @Input
    public String getNativeBuilderImage() {
        return nativeBuilderImage;
    }

    @Input
    public boolean isEnableVMInspection() {
        return enableVMInspection;
    }

    @Option(description = "Enable VM inspection", option = "enable-vm-inspection")
    public void setEnableVMInspection(boolean enableVMInspection) {
        this.enableVMInspection = enableVMInspection;
    }

    @Input
    public boolean isFullStackTraces() {
        return fullStackTraces;
    }

    @Option(description = "Specify full stacktraces", option = "full-stacktraces")
    public void setFullStackTraces(boolean fullStackTraces) {
        this.fullStackTraces = fullStackTraces;
    }

    @Input
    public boolean isEnableReports() {
        return enableReports;
    }

    @Deprecated
    @Option(description = "Disable reports", option = "disable-reports")
    public void setDisableReports(boolean disableReports) {
        this.enableReports = !disableReports;
    }

    @Option(description = "Enable reports", option = "enable-reports")
    public void setEnableReports(boolean enableReports) {
        this.enableReports = enableReports;
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

        final AppArtifact appArtifact = extension().getAppArtifact();
        final AppModelResolver modelResolver = extension().getAppModelResolver();
        try {
            modelResolver.resolveModel(appArtifact);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
        final Properties realProperties = getBuildSystemProperties(appArtifact);

        Map<String, String> config = createCustomConfig();
        Map<String, String> old = new HashMap<>();
        for (Map.Entry<String, String> e : config.entrySet()) {
            old.put(e.getKey(), System.getProperty(e.getKey()));
            System.setProperty(e.getKey(), e.getValue());
        }
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder(appArtifact.getPath())
                .setAppModelResolver(modelResolver)
                .setBaseClassLoader(getClass().getClassLoader())
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setLocalProjectDiscovery(false)
                .setBuildSystemProperties(realProperties)
                .setIsolateDeployment(true)
                .setAppArtifact(appArtifact)
                .build().bootstrap()) {
            appCreationContext.createAugmentor().createProductionApplication();

        } catch (BootstrapException e) {
            throw new GradleException("Failed to build a runnable JAR", e);
        } finally {
            for (Map.Entry<String, String> e : old.entrySet()) {
                if (e.getValue() == null) {
                    System.clearProperty(e.getKey());
                } else {
                    System.setProperty(e.getKey(), e.getValue());
                }
            }
        }
    }

    private Map<String, String> createCustomConfig() {
        Map<String, String> configs = new HashMap<>();
        configs.put("quarkus.package.type", "native");

        configs.put("quarkus.native.add-all-charsets", Boolean.toString(addAllCharsets));

        if (additionalBuildArgs != null && !additionalBuildArgs.isEmpty()) {
            configs.put("quarkus.native.additional-build-args",
                    additionalBuildArgs.stream()
                            .map(val -> val.replace("\\", "\\\\"))
                            .map(val -> val.replace(",", "\\,"))
                            .collect(joining(",")));
        }
        configs.put("quarkus.native.auto-service-loader-registration", Boolean.toString(autoServiceLoaderRegistration));

        configs.put("quarkus.native.cleanup-server", Boolean.toString(cleanupServer));
        configs.put("quarkus.native.debug-build-process", Boolean.toString(debugBuildProcess));

        configs.put("quarkus.native.debug-symbols", Boolean.toString(debugSymbols));
        configs.put("quarkus.native.enable-reports", Boolean.toString(enableReports));
        if (containerRuntime != null && !containerRuntime.trim().isEmpty()) {
            configs.put("quarkus.native.container-runtime", containerRuntime);
        } else if (dockerBuild != null && !dockerBuild.trim().isEmpty()) {
            if (!dockerBuild.isEmpty() && !dockerBuild.toLowerCase().equals("false")) {
                if (dockerBuild.toLowerCase().equals("true")) {
                    configs.put("quarkus.native.container-runtime", "docker");
                } else {
                    configs.put("quarkus.native.container-runtime", dockerBuild);
                }
            }
        }
        if (containerRuntimeOptions != null && !containerRuntimeOptions.trim().isEmpty()) {
            configs.put("quarkus.native.container-runtime-options", containerRuntimeOptions);
        }
        if (nativeBuilderImage != null && !nativeBuilderImage.trim().isEmpty()) {
            configs.put("quarkus.native.builder-image", nativeBuilderImage);
        }
        configs.put("quarkus.native.dump-proxies", Boolean.toString(dumpProxies));
        configs.put("quarkus.native.enable-all-security-services", Boolean.toString(enableAllSecurityServices));
        configs.put("quarkus.native.enable-fallback-images", Boolean.toString(enableFallbackImages));
        configs.put("quarkus.native.enable-https-url-handler", Boolean.toString(enableHttpsUrlHandler));

        configs.put("quarkus.native.enable-http-url-handler", Boolean.toString(enableHttpUrlHandler));
        configs.put("quarkus.native.enable-isolates", Boolean.toString(enableIsolates));

        configs.put("quarkus.native.enable-server", Boolean.toString(enableServer));

        configs.put("quarkus.native.enable-vm-inspection", Boolean.toString(enableVMInspection));

        configs.put("quarkus.native.full-stack-traces", Boolean.toString(fullStackTraces));

        if (graalvmHome != null && !graalvmHome.trim().isEmpty()) {
            configs.put("quarkus.native.graalvm-home", graalvmHome);
        }
        if (nativeImageXmx != null && !nativeImageXmx.trim().isEmpty()) {
            configs.put("quarkus.native.native-image-xmx", nativeImageXmx);
        }
        configs.put("quarkus.native.report-errors-at-runtime", Boolean.toString(reportErrorsAtRuntime));

        configs.put("quarkus.native.report-exception-stack-traces", Boolean.toString(reportExceptionStackTraces));

        return configs;

    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name) {
            this.ordinal = ordinal;
            this.name = name;
        }

        public InMemoryConfigSource add(String key, String value) {
            values.put(key, value);
            return this;
        }

        public InMemoryConfigSource add(String key, Object value) {
            values.put(key, value.toString());
            return this;
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
