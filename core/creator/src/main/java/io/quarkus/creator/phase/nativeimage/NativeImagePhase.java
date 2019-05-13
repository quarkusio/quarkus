/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.phase.nativeimage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.config.reader.PropertyContext;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The outcome of this phase is a native image
 *
 * @author Alexey Loubyansky
 */
public class NativeImagePhase implements AppCreationPhase<NativeImagePhase>, NativeImageOutcome {

    private static final Logger log = Logger.getLogger(NativeImagePhase.class);

    private static final String GRAALVM_HOME = "GRAALVM_HOME";

    private static final String QUARKUS_PREFIX = "quarkus.";

    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private Path outputDir;

    private boolean reportErrorsAtRuntime;

    private boolean debugSymbols;

    private boolean debugBuildProcess;

    private boolean cleanupServer;

    private boolean enableHttpUrlHandler;

    private boolean enableHttpsUrlHandler;

    private boolean enableAllSecurityServices;

    private boolean enableRetainedHeapReporting;

    private boolean enableCodeSizeReporting;

    private boolean enableIsolates;

    private boolean enableFallbackImages;

    private String graalvmHome;

    private boolean enableServer;

    private boolean enableJni;

    private boolean autoServiceLoaderRegistration;

    private boolean dumpProxies;

    private String nativeImageXmx;

    private String builderImage = "quay.io/quarkus/centos-quarkus-native-image:graalvm-1.0.0-rc16";

    private String containerRuntime = "";

    private List<String> containerRuntimeOptions = new ArrayList<>();

    private boolean enableVMInspection;

    private boolean fullStackTraces;

    private boolean disableReports;

    private List<String> additionalBuildArgs;

    private boolean addAllCharsets;

    private boolean reportExceptionStackTraces = true;

    public NativeImagePhase setAddAllCharsets(boolean addAllCharsets) {
        this.addAllCharsets = addAllCharsets;
        return this;
    }

    public NativeImagePhase setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public NativeImagePhase setReportErrorsAtRuntime(boolean reportErrorsAtRuntime) {
        this.reportErrorsAtRuntime = reportErrorsAtRuntime;
        return this;
    }

    public NativeImagePhase setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
        return this;
    }

    public NativeImagePhase setDebugBuildProcess(boolean debugBuildProcess) {
        this.debugBuildProcess = debugBuildProcess;
        return this;
    }

    public NativeImagePhase setCleanupServer(boolean cleanupServer) {
        this.cleanupServer = cleanupServer;
        return this;
    }

    public NativeImagePhase setEnableHttpUrlHandler(boolean enableHttpUrlHandler) {
        this.enableHttpUrlHandler = enableHttpUrlHandler;
        return this;
    }

    public NativeImagePhase setEnableHttpsUrlHandler(boolean enableHttpsUrlHandler) {
        this.enableHttpsUrlHandler = enableHttpsUrlHandler;
        return this;
    }

    public NativeImagePhase setEnableAllSecurityServices(boolean enableAllSecurityServices) {
        this.enableAllSecurityServices = enableAllSecurityServices;
        return this;
    }

    public NativeImagePhase setEnableRetainedHeapReporting(boolean enableRetainedHeapReporting) {
        this.enableRetainedHeapReporting = enableRetainedHeapReporting;
        return this;
    }

    public NativeImagePhase setEnableCodeSizeReporting(boolean enableCodeSizeReporting) {
        this.enableCodeSizeReporting = enableCodeSizeReporting;
        return this;
    }

    public NativeImagePhase setEnableIsolates(boolean enableIsolates) {
        this.enableIsolates = enableIsolates;
        return this;
    }

    public NativeImagePhase setEnableFallbackImages(boolean enableFallbackImages) {
        this.enableFallbackImages = enableFallbackImages;
        return this;
    }

    public NativeImagePhase setGraalvmHome(String graalvmHome) {
        this.graalvmHome = graalvmHome;
        return this;
    }

    public NativeImagePhase setEnableServer(boolean enableServer) {
        this.enableServer = enableServer;
        return this;
    }

    public NativeImagePhase setEnableJni(boolean enableJni) {
        this.enableJni = enableJni;
        return this;
    }

    public NativeImagePhase setAutoServiceLoaderRegistration(boolean autoServiceLoaderRegistration) {
        this.autoServiceLoaderRegistration = autoServiceLoaderRegistration;
        return this;
    }

    public NativeImagePhase setDumpProxies(boolean dumpProxies) {
        this.dumpProxies = dumpProxies;
        return this;
    }

    public NativeImagePhase setNativeImageXmx(String nativeImageXmx) {
        this.nativeImageXmx = nativeImageXmx;
        return this;
    }

    public NativeImagePhase setDockerBuild(String dockerBuild) {
        if (dockerBuild == null) {
            return this;
        }

        if ("false".equals(dockerBuild.toLowerCase())) {
            this.containerRuntime = "";
        } else {
            this.containerRuntime = "docker";

            // TODO: use an 'official' image
            if (!"true".equals(dockerBuild.toLowerCase())) {
                this.builderImage = dockerBuild;
            }
        }

        return this;
    }

    public NativeImagePhase setContainerRuntime(String containerRuntime) {
        if (containerRuntime == null) {
            return this;
        }
        if ("podman".equals(containerRuntime) || "docker".equals(containerRuntime)) {
            this.containerRuntime = containerRuntime;
        } else {
            log.warn("container runtime is not docker or podman. fallback to docker");
            this.containerRuntime = "docker";
        }
        return this;
    }

    public NativeImagePhase setContainerRuntimeOptions(String containerRuntimeOptions) {
        if (containerRuntimeOptions != null) {
            this.containerRuntimeOptions = Arrays.asList(containerRuntimeOptions.split(","));
        }
        return this;
    }

    public NativeImagePhase setEnableVMInspection(boolean enableVMInspection) {
        this.enableVMInspection = enableVMInspection;
        return this;
    }

    public NativeImagePhase setFullStackTraces(boolean fullStackTraces) {
        this.fullStackTraces = fullStackTraces;
        return this;
    }

    public NativeImagePhase setDisableReports(boolean disableReports) {
        this.disableReports = disableReports;
        return this;
    }

    public NativeImagePhase setAdditionalBuildArgs(List<String> additionalBuildArgs) {
        this.additionalBuildArgs = additionalBuildArgs;
        return this;
    }

    public NativeImagePhase setReportExceptionStackTraces(boolean reportExceptionStackTraces) {
        this.reportExceptionStackTraces = reportExceptionStackTraces;
        return this;
    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(NativeImageOutcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {

        outputDir = outputDir == null ? ctx.getWorkPath() : IoUtils.mkdirs(outputDir);

        final RunnerJarOutcome runnerJarOutcome = ctx.resolveOutcome(RunnerJarOutcome.class);
        Path runnerJar = runnerJarOutcome.getRunnerJar();
        boolean runnerJarCopied = false;
        // this trick is here because docker needs the jar in the project dir
        if (!runnerJar.getParent().equals(outputDir)) {
            try {
                runnerJar = IoUtils.copy(runnerJar, outputDir.resolve(runnerJar.getFileName()));
            } catch (IOException e) {
                throw new AppCreatorException("Failed to copy the runnable jar to the output dir", e);
            }
            runnerJarCopied = true;
        }
        final String runnerJarName = runnerJar.getFileName().toString();

        Path outputLibDir = outputDir.resolve(runnerJarOutcome.getLibDir().getFileName());
        boolean outputLibDirCopied = false;
        if (Files.exists(outputLibDir)) {
            outputLibDir = null;
        } else {
            try {
                IoUtils.copy(runnerJarOutcome.getLibDir(), outputLibDir);
            } catch (IOException e) {
                throw new AppCreatorException("Failed to copy the runnable jar and the lib to the docker project dir", e);
            }
            outputLibDirCopied = true;
        }

        final Config config = SmallRyeConfigProviderResolver.instance().getConfig();

        boolean vmVersionOutOfDate = isThisGraalVMRCObsolete();

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;

        String noPIE = "";

        if (!"".equals(containerRuntime)) {
            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm quarkus/graalvm-native-image"
            nativeImage = new ArrayList<>();
            Collections.addAll(nativeImage, containerRuntime, "run", "-v", outputDir.toAbsolutePath() + ":/project:z", "--rm");

            if (IS_LINUX & "docker".equals(containerRuntime)) {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null & gid != null & !"".equals(uid) & !"".equals(gid)) {
                    Collections.addAll(nativeImage, "--user", uid.concat(":").concat(gid));
                }
            }
            nativeImage.addAll(containerRuntimeOptions);
            nativeImage.add(this.builderImage);
        } else {
            if (IS_LINUX) {
                noPIE = detectNoPIE();
            }

            String graalvmHome = this.graalvmHome;
            if (graalvmHome != null) {
                env.put(GRAALVM_HOME, graalvmHome);
            } else {
                graalvmHome = env.get(GRAALVM_HOME);
                if (graalvmHome == null) {
                    throw new AppCreatorException("GRAALVM_HOME was not set");
                }
            }
            String imageName = IS_WINDOWS ? "native-image.cmd" : "native-image";
            nativeImage = Collections.singletonList(graalvmHome + File.separator + "bin" + File.separator + imageName);

        }

        try {
            List<String> command = new ArrayList<>(nativeImage);
            if (cleanupServer) {
                List<String> cleanup = new ArrayList<>(nativeImage);
                cleanup.add("--server-shutdown");
                ProcessBuilder pb = new ProcessBuilder(cleanup.toArray(new String[0]));
                pb.directory(outputDir.toFile());
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process process = pb.start();
                process.waitFor();
            }
            // TODO this is a temp hack
            final Path propsFile = ctx.resolveOutcome(AugmentOutcome.class).getAppClassesDir()
                    .resolve("native-image.properties");

            boolean enableSslNative = false;
            if (Files.exists(propsFile)) {
                final Properties properties = new Properties();
                try (BufferedReader reader = Files.newBufferedReader(propsFile, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
                for (String propertyName : properties.stringPropertyNames()) {
                    if (propertyName.startsWith(QUARKUS_PREFIX)) {
                        continue;
                    }

                    final String propertyValue = properties.getProperty(propertyName);
                    // todo maybe just -D is better than -J-D in this case
                    if (propertyValue == null) {
                        command.add("-J-D" + propertyName);
                    } else {
                        command.add("-J-D" + propertyName + "=" + propertyValue);
                    }
                }

                enableSslNative = properties.getProperty("quarkus.ssl.native") != null
                        ? Boolean.parseBoolean(properties.getProperty("quarkus.ssl.native"))
                        : false;
            }
            if (enableSslNative) {
                enableHttpsUrlHandler = true;
                enableJni = true;
                enableAllSecurityServices = true;
            }
            if (additionalBuildArgs != null) {
                command.addAll(additionalBuildArgs);
            }
            command.add("-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
            command.add("-jar");
            command.add(runnerJarName);
            //https://github.com/oracle/graal/issues/660
            command.add("-J-Djava.util.concurrent.ForkJoinPool.common.parallelism=1");
            if (enableFallbackImages) {
                command.add("-H:FallbackThreshold=5");
            } else {
                //Default: be strict as those fallback images aren't very useful
                //and tend to cover up real problems.
                command.add("-H:FallbackThreshold=0");
            }

            if (reportErrorsAtRuntime) {
                command.add("-H:+ReportUnsupportedElementsAtRuntime");
            }
            if (reportExceptionStackTraces) {
                command.add("-H:+ReportExceptionStackTraces");
            }
            if (debugSymbols) {
                command.add("-g");
            }
            if (debugBuildProcess) {
                command.add("-J-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y");
            }
            if (!disableReports) {
                command.add("-H:+PrintAnalysisCallTree");
            }
            if (dumpProxies) {
                command.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
                if (enableServer) {
                    log.warn(
                            "Options dumpProxies and enableServer are both enabled: this will get the proxies dumped in an unknown external working directory");
                }
            }
            if (nativeImageXmx != null) {
                command.add("-J-Xmx" + nativeImageXmx);
            }
            List<String> protocols = new ArrayList<>(2);
            if (enableHttpUrlHandler) {
                protocols.add("http");
            }
            if (enableHttpsUrlHandler) {
                protocols.add("https");
            }
            if (addAllCharsets) {
                command.add("-H:+AddAllCharsets");
            } else {
                command.add("-H:-AddAllCharsets");
            }
            if (!protocols.isEmpty()) {
                command.add("-H:EnableURLProtocols=" + String.join(",", protocols));
            }
            if (enableAllSecurityServices) {
                command.add("--enable-all-security-services");
            }
            if (!noPIE.isEmpty()) {
                command.add("-H:NativeLinkerOption=" + noPIE);
            }
            if (enableRetainedHeapReporting) {
                command.add("-H:+PrintRetainedHeapHistogram");
            }
            if (enableCodeSizeReporting) {
                command.add("-H:+PrintCodeSizeReport");
            }
            if (!enableIsolates) {
                command.add("-H:-SpawnIsolates");
            }
            if (enableJni) {
                command.add("-H:+JNI");
            } else {
                command.add("-H:-JNI");
            }
            if (!enableServer && !IS_WINDOWS) {
                command.add("--no-server");
            }
            if (enableVMInspection) {
                command.add("-H:+AllowVMInspection");
            }
            if (autoServiceLoaderRegistration) {
                command.add("-H:+UseServiceLoaderFeature");
                //When enabling, at least print what exactly is being added:
                command.add("-H:+TraceServiceLoaderFeature");
            } else {
                command.add("-H:-UseServiceLoaderFeature");
            }
            if (fullStackTraces) {
                command.add("-H:+StackTrace");
            } else {
                command.add("-H:-StackTrace");
            }

            log.info(command.stream().collect(Collectors.joining(" ")));
            CountDownLatch errorReportLatch = new CountDownLatch(1);

            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
            pb.directory(outputDir.toFile());
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();
            new Thread(new ErrorReplacingProcessReader(process.getErrorStream(), outputDir.resolve("reports").toFile(),
                    errorReportLatch)).start();
            errorReportLatch.await();
            if (process.waitFor() != 0) {
                throw new RuntimeException("Image generation failed");
            }
            System.setProperty("native.image.path", runnerJarName.substring(0, runnerJarName.lastIndexOf('.')));

            ctx.pushOutcome(NativeImageOutcome.class, this);
        } catch (Exception e) {
            throw new AppCreatorException("Failed to build native image", e);
        } finally {
            if (runnerJarCopied) {
                IoUtils.recursiveDelete(runnerJar);
            }
            if (outputLibDirCopied) {
                IoUtils.recursiveDelete(outputLibDir);
            }
        }
    }

    //FIXME remove after transition period
    private boolean isThisGraalVMRCObsolete() {
        final String vmName = System.getProperty("java.vm.name");
        log.info("Running Quarkus native-image plugin on " + vmName);
        final List<String> obsoleteGraalVmVersions = Arrays.asList("-rc9", "-rc10", "-rc11", "-rc12", "-rc13", "-rc14",
                "-rc15");
        final boolean vmVersionIsObsolete = obsoleteGraalVmVersions.stream().anyMatch(vmName::contains);
        if (vmVersionIsObsolete) {
            log.error("Out of date RC build of GraalVM detected! Please upgrade to GraalVM RC16");
            return true;
        }
        return false;
    }

    private static String getLinuxID(String option) {
        Process process;

        try {
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            ProcessBuilder idPB = new ProcessBuilder().command("id", option);
            idPB.redirectError(new File("/dev/null"));
            idPB.redirectInput(new File("/dev/null"));

            process = idPB.start();
            try (InputStream inputStream = process.getInputStream()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    safeWaitFor(process);
                    return responseBuilder.toString();
                }
            } catch (Throwable t) {
                safeWaitFor(process);
                throw t;
            }
        } catch (IOException e) { //from process.start()
            //swallow and return null id
            return null;
        }
    }

    static void safeWaitFor(Process process) {
        boolean intr = false;
        try {
            for (;;)
                try {
                    process.waitFor();
                    return;
                } catch (InterruptedException ex) {
                    intr = true;
                }
        } finally {
            if (intr)
                Thread.currentThread().interrupt();
        }
    }

    private static String detectNoPIE() {
        String argument = testGCCArgument("-no-pie");

        return argument.length() == 0 ? testGCCArgument("-nopie") : argument;
    }

    private static String testGCCArgument(String argument) {
        try {
            Process gcc = new ProcessBuilder("cc", "-v", "-E", argument, "-").start();
            gcc.getOutputStream().close();
            if (gcc.waitFor() == 0) {
                return argument;
            }

        } catch (IOException | InterruptedException e) {
            // eat
        }

        return "";
    }

    @Override
    public String getConfigPropertyName() {
        return "native-image";
    }

    @Override
    public PropertiesHandler<NativeImagePhase> getPropertiesHandler() {
        return new PropertiesHandler<NativeImagePhase>() {
            @Override
            public NativeImagePhase getTarget() {
                return NativeImagePhase.this;
            }

            @Override
            public boolean set(NativeImagePhase t, PropertyContext ctx) {
                //System.out.println("native-image.set " + ctx.getRelativeName() + "=" + ctx.getValue());
                final String value = ctx.getValue();
                switch (ctx.getRelativeName()) {
                    case "output":
                        t.setOutputDir(Paths.get(value));
                        break;
                    case "report-errors-at-runtime":
                        t.setReportErrorsAtRuntime(Boolean.parseBoolean(value));
                        break;
                    case "debug-symbols":
                        t.setDebugSymbols(Boolean.parseBoolean(value));
                        break;
                    case "debug-build-process":
                        t.setDebugBuildProcess(Boolean.parseBoolean(value));
                        break;
                    case "cleanup-server":
                        t.setCleanupServer(Boolean.parseBoolean(value));
                        break;
                    case "enable-http-url-handler":
                        t.setEnableHttpUrlHandler(Boolean.parseBoolean(value));
                        break;
                    case "enable-https-url-handler":
                        t.setEnableHttpsUrlHandler(Boolean.parseBoolean(value));
                        break;
                    case "enable-all-security-services":
                        t.setEnableAllSecurityServices(Boolean.parseBoolean(value));
                        break;
                    case "enable-retained-heap-reporting":
                        t.setEnableRetainedHeapReporting(Boolean.parseBoolean(value));
                        break;
                    case "enable-code-size-reporting":
                        t.setEnableCodeSizeReporting(Boolean.parseBoolean(value));
                        break;
                    case "enable-isolates":
                        t.setEnableIsolates(Boolean.parseBoolean(value));
                        break;
                    case "enable-fallback-images":
                        t.setEnableFallbackImages(Boolean.parseBoolean(value));
                        break;
                    case "graalvm-home":
                        t.setGraalvmHome(value);
                        break;
                    case "enable-server":
                        t.setEnableServer(Boolean.parseBoolean(value));
                        break;
                    case "enable-jni":
                        t.setEnableJni(Boolean.parseBoolean(value));
                        break;
                    case "auto-service-loader-registration":
                        t.setAutoServiceLoaderRegistration(Boolean.parseBoolean(value));
                        break;
                    case "dump-proxies":
                        t.setDumpProxies(Boolean.parseBoolean(value));
                        break;
                    case "native-image-xmx":
                        t.setNativeImageXmx(value);
                        break;
                    case "docker-build":
                        t.setDockerBuild(value);
                        break;
                    case "enable-vm-inspection":
                        t.setEnableVMInspection(Boolean.parseBoolean(value));
                        break;
                    case "full-stack-traces":
                        t.setFullStackTraces(Boolean.parseBoolean(value));
                        break;
                    case "disable-reports":
                        t.setDisableReports(Boolean.parseBoolean(value));
                        break;
                    case "additional-build-args":
                        t.setAdditionalBuildArgs(Arrays.asList(value.split(",")));
                        break;
                    case "report-exception-stack-traces":
                        t.setReportExceptionStackTraces(Boolean.parseBoolean(value));
                        break;
                    default:
                        return false;
                }
                return true;
            }
        };
    }
}
