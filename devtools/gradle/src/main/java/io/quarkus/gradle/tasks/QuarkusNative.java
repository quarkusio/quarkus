/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.gradle.tasks;

import java.nio.file.Path;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;
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

    private String dockerBuild;

    private boolean enableVMInspection = false;

    private boolean fullStackTraces = true;

    private boolean disableReports;

    private List<String> additionalBuildArgs;

    private boolean addAllCharsets = false;

    public QuarkusNative() {
        super("Building a native image");
    }

    public boolean isAddAllCharsets() {
        return addAllCharsets;
    }

    @Option(description = "Should all Charsets supported by the host environment be included in the native image", option = "add-all-charsets")
    @Optional
    public void setAddAllCharsets(final boolean addAllCharsets) {
        this.addAllCharsets = addAllCharsets;
    }

    public boolean isReportErrorsAtRuntime() {
        return reportErrorsAtRuntime;
    }

    @Option(description = "Report errors at runtime", option = "report-errors-runtime")
    @Optional
    public void setReportErrorsAtRuntime(boolean reportErrorsAtRuntime) {
        this.reportErrorsAtRuntime = reportErrorsAtRuntime;
    }

    public boolean isDebugSymbols() {
        return debugSymbols;
    }

    @Option(description = "Specify if debug symbols should be set", option = "debug-symbols")
    @Optional
    public void setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    public boolean isDebugBuildProcess() {
        return debugBuildProcess;
    }

    @Option(description = "Specify if debug is set during build process", option = "debug-build-process")
    @Optional
    public void setDebugBuildProcess(boolean debugBuildProcess) {
        this.debugBuildProcess = debugBuildProcess;
    }

    public boolean isCleanupServer() {
        return cleanupServer;
    }

    @Option(description = "Cleanup server", option = "cleanup-server")
    @Optional
    public void setCleanupServer(boolean cleanupServer) {
        this.cleanupServer = cleanupServer;
    }

    public boolean isEnableHttpUrlHandler() {
        return enableHttpUrlHandler;
    }

    @Option(description = "Specify if http url handler is enabled", option = "enable-http-url-handler")
    @Optional
    public void setEnableHttpUrlHandler(boolean enableHttpUrlHandler) {
        this.enableHttpUrlHandler = enableHttpUrlHandler;
    }

    public boolean isEnableHttpsUrlHandler() {
        return enableHttpsUrlHandler;
    }

    @Option(description = "Specify if https url handler is enabled", option = "enable-https-url-handler")
    @Optional
    public void setEnableHttpsUrlHandler(boolean enableHttpsUrlHandler) {
        this.enableHttpsUrlHandler = enableHttpsUrlHandler;
    }

    public boolean isEnableAllSecurityServices() {
        return enableAllSecurityServices;
    }

    @Option(description = "Enable all security services", option = "enable-all-security-services")
    @Optional
    public void setEnableAllSecurityServices(boolean enableAllSecurityServices) {
        this.enableAllSecurityServices = enableAllSecurityServices;
    }

    public boolean isEnableRetainedHeapReporting() {
        return enableRetainedHeapReporting;
    }

    @Option(description = "Specify if retained heap reporting should be enabled", option = "enable-retained-heap-reporting")
    @Optional
    public void setEnableRetainedHeapReporting(boolean enableRetainedHeapReporting) {
        this.enableRetainedHeapReporting = enableRetainedHeapReporting;
    }

    public boolean isEnableIsolates() {
        return enableIsolates;
    }

    @Option(description = "Report errors at runtime", option = "enable-isolates")
    @Optional
    public void setEnableIsolates(boolean enableIsolates) {
        this.enableIsolates = enableIsolates;
    }

    public boolean isEnableCodeSizeReporting() {
        return enableCodeSizeReporting;
    }

    @Option(description = "Report errors at runtime", option = "enable-code-size-reporting")
    @Optional
    public void setEnableCodeSizeReporting(boolean enableCodeSizeReporting) {
        this.enableCodeSizeReporting = enableCodeSizeReporting;
    }

    public String getGraalvmHome() {
        if (graalvmHome == null || graalvmHome.length() < 1)
            throw new GradleException(
                    "The GRAALVM_HOME environment variable need to be set to your GraalVM root directory to use native mode");
        return graalvmHome;
    }

    @Option(description = "Specify the GraalVM directory (default to $GRAALVM_HOME)", option = "graalvm")
    @Optional
    public void setGraalvmHome(String graalvmHome) {
        this.graalvmHome = graalvmHome;
    }

    public boolean isEnableServer() {
        return enableServer;
    }

    @Option(description = "Enable server", option = "enable-server")
    @Optional
    public void setEnableServer(boolean enableServer) {
        this.enableServer = enableServer;
    }

    public boolean isEnableJni() {
        return enableJni;
    }

    @Option(description = "Enable jni", option = "enable-jni")
    @Optional
    public void setEnableJni(boolean enableJni) {
        this.enableJni = enableJni;
    }

    public boolean isAutoServiceLoaderRegistration() {
        return autoServiceLoaderRegistration;
    }

    @Option(description = "Auto ServiceLoader registration", option = "auto-serviceloader-registration")
    @Optional
    public void setAutoServiceLoaderRegistration(boolean autoServiceLoaderRegistration) {
        this.autoServiceLoaderRegistration = autoServiceLoaderRegistration;
    }

    public boolean isDumpProxies() {
        return dumpProxies;
    }

    @Option(description = "Dump proxies", option = "dump-proxies")
    @Optional
    public void setDumpProxies(boolean dumpProxies) {
        this.dumpProxies = dumpProxies;
    }

    public String getNativeImageXmx() {
        return nativeImageXmx;
    }

    @Option(description = "Specify the native image maximum heap size", option = "native-image-xmx")
    @Optional
    public void setNativeImageXmx(String nativeImageXmx) {
        this.nativeImageXmx = nativeImageXmx;
    }

    public String getDockerBuild() {
        return dockerBuild;
    }

    @Option(description = "Docker build", option = "docker-build")
    @Optional
    public void setDockerBuild(String dockerBuild) {
        this.dockerBuild = dockerBuild;
    }

    public boolean isEnableVMInspection() {
        return enableVMInspection;
    }

    @Option(description = "Enable VM inspection", option = "enable-vm-inspection")
    @Optional
    public void setEnableVMInspection(boolean enableVMInspection) {
        this.enableVMInspection = enableVMInspection;
    }

    public boolean isFullStackTraces() {
        return fullStackTraces;
    }

    @Option(description = "Specify full stacktraces", option = "full-stacktraces")
    @Optional
    public void setFullStackTraces(boolean fullStackTraces) {
        this.fullStackTraces = fullStackTraces;
    }

    public boolean isDisableReports() {
        return disableReports;
    }

    @Option(description = "Disable reports", option = "disable-reports")
    @Optional
    public void setDisableReports(boolean disableReports) {
        this.disableReports = disableReports;
    }

    public List<String> getAdditionalBuildArgs() {
        return additionalBuildArgs;
    }

    @Option(description = "Additional build arguments", option = "additional-build-args")
    @Optional
    public void setAdditionalBuildArgs(List<String> additionalBuildArgs) {
        this.additionalBuildArgs = additionalBuildArgs;
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
                        .setFullStackTraces(isFullStackTraces())
                        .setGraalvmHome(getGraalvmHome())
                        .setNativeImageXmx(getNativeImageXmx())
                        .setReportErrorsAtRuntime(isReportErrorsAtRuntime()))

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
                public boolean isWhitelisted(AppDependency dep) {
                    // not relevant for this mojo
                    throw new UnsupportedOperationException();
                }
            }).pushOutcome(RunnerJarOutcome.class, new RunnerJarOutcome() {
                final Path runnerJar = getProject().getBuildDir().toPath().resolve(extension().finalName() + "-runner.jar");

                @Override
                public Path getRunnerJar() {
                    return runnerJar;
                }

                @Override
                public Path getLibDir() {
                    return runnerJar.getParent().resolve("lib");
                }
            }).resolveOutcome(NativeImageOutcome.class);

        } catch (AppCreatorException e) {
            throw new GradleException("Failed to generate a native image", e);
        }

    }
}
