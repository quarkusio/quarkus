package io.quarkus.gradle.tasks;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.phase.augment.AugmentTask;

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

    private boolean enableIsolates;

    private String graalvmHome = System.getenv("GRAALVM_HOME");

    private boolean enableServer = false;

    private boolean enableJni = false;

    private boolean autoServiceLoaderRegistration = false;

    private boolean dumpProxies = false;

    private String nativeImageXmx;

    private String containerRuntime;

    private String containerRuntimeOptions;

    private String dockerBuild;

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

    @Option(description = "Auto ServiceLoader registration", option = "auto-service-loader-registration")
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

        final AppArtifact appArtifact = extension().getAppArtifact();
        final AppModelResolver modelResolver = extension().resolveAppModel();
        try {
            modelResolver.resolveModel(appArtifact);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
        final Map<String, ?> properties = getProject().getProperties();
        final Properties realProperties = new Properties();
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null && value instanceof String && key.startsWith("quarkus.")) {
                realProperties.setProperty(key, (String) value);
            }
        }
        realProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        realProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        try (CuratedApplicationCreator appCreationContext = CuratedApplicationCreator.builder()
                .setWorkDir(getProject().getBuildDir().toPath())
                .setModelResolver(modelResolver)
                .setBaseName(extension().finalName())
                .setAppArtifact(appArtifact).build()) {

            AugmentTask task = AugmentTask.builder().setBuildSystemProperties(realProperties)
                    .setConfigCustomizer(createCustomConfig())
                    .setAppClassesDir(extension().outputDirectory().toPath())
                    .setConfigDir(extension().outputConfigDirectory().toPath()).build();
            appCreationContext.runTask(task);
        } catch (AppCreatorException e) {
            throw new GradleException("Failed to generate a native image", e);
        }

    }

    private Consumer<ConfigBuilder> createCustomConfig() {
        return new Consumer<ConfigBuilder>() {
            @Override
            public void accept(ConfigBuilder configBuilder) {
                InMemoryConfigSource type = new InMemoryConfigSource(Integer.MAX_VALUE, "Native Image Type")
                        .add("quarkus.package.type", "native");

                InMemoryConfigSource configs = new InMemoryConfigSource(0, "Native Image Maven Settings");

                configs.add("quarkus.native.add-all-charsets", addAllCharsets);

                if (additionalBuildArgs != null && !additionalBuildArgs.isEmpty()) {
                    configs.add("quarkus.native.additional-build-args",
                            additionalBuildArgs.stream()
                                    .map(val -> val.replace("\\", "\\\\"))
                                    .map(val -> val.replace(",", "\\,"))
                                    .collect(joining(",")));
                }
                configs.add("quarkus.native.auto-service-loader-registration", autoServiceLoaderRegistration);

                configs.add("quarkus.native.cleanup-server", cleanupServer);
                configs.add("quarkus.native.debug-build-process", debugBuildProcess);

                configs.add("quarkus.native.debug-symbols", debugSymbols);
                configs.add("quarkus.native.enable-reports", enableReports);
                if (containerRuntime != null && !containerRuntime.trim().isEmpty()) {
                    configs.add("quarkus.native.container-runtime", containerRuntime);
                } else if (dockerBuild != null && !dockerBuild.trim().isEmpty()) {
                    if (!dockerBuild.isEmpty() && !dockerBuild.toLowerCase().equals("false")) {
                        if (dockerBuild.toLowerCase().equals("true")) {
                            configs.add("quarkus.native.container-runtime", "docker");
                        } else {
                            configs.add("quarkus.native.container-runtime", dockerBuild);
                        }
                    }
                }
                if (containerRuntimeOptions != null && !containerRuntimeOptions.trim().isEmpty()) {
                    configs.add("quarkus.native.container-runtime-options", containerRuntimeOptions);
                }
                configs.add("quarkus.native.dump-proxies", dumpProxies);
                configs.add("quarkus.native.enable-all-security-services", enableAllSecurityServices);
                configs.add("quarkus.native.enable-fallback-images", enableFallbackImages);
                configs.add("quarkus.native.enable-https-url-handler", enableHttpsUrlHandler);

                configs.add("quarkus.native.enable-http-url-handler", enableHttpUrlHandler);
                configs.add("quarkus.native.enable-isolates", enableIsolates);
                configs.add("quarkus.native.enable-jni", enableJni);

                configs.add("quarkus.native.enable-server", enableServer);

                configs.add("quarkus.native.enable-vm-inspection", enableVMInspection);

                configs.add("quarkus.native.full-stack-traces", fullStackTraces);

                if (graalvmHome != null && !graalvmHome.trim().isEmpty()) {
                    configs.add("quarkus.native.graalvm-home", graalvmHome);
                }
                if (nativeImageXmx != null && !nativeImageXmx.trim().isEmpty()) {
                    configs.add("quarkus.native.native-image-xmx", nativeImageXmx);
                }
                configs.add("quarkus.native.report-errors-at-runtime", reportErrorsAtRuntime);

                configs.add("quarkus.native.report-exception-stack-traces", reportExceptionStackTraces);

                configBuilder.withSources(type, configs);
            }
        };

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
