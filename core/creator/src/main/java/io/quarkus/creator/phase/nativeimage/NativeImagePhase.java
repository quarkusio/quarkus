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
import java.io.PrintStream;
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

import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.config.reader.PropertyContext;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;
import io.quarkus.creator.util.IoUtils;
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

    private String graalvmHome;

    private boolean enableServer;

    private boolean enableJni;

    private boolean autoServiceLoaderRegistration;

    private boolean dumpProxies;

    private String nativeImageXmx;

    private String dockerBuild;

    private boolean enableVMInspection;

    private boolean fullStackTraces;

    private boolean disableReports;

    private List<String> additionalBuildArgs;

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
        this.dockerBuild = dockerBuild;
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

        if (dockerBuild != null && !dockerBuild.toLowerCase().equals("false")) {

            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm quarkus/graalvm-native-image"
            nativeImage = new ArrayList<>();
            //TODO: use an 'official' image
            String image;
            if (dockerBuild.toLowerCase().equals("true")) {
                image = "swd847/centos-graal-native-image-rc12";
            } else {
                //allow the use of a custom image
                image = dockerBuild;
            }
            Collections.addAll(nativeImage, "docker", "run", "-v", outputDir.toAbsolutePath() + ":/project:z", "--rm", image);
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
            nativeImage = Collections.singletonList(graalvmHome + File.separator + "bin" + File.separator + "native-image");
        }

        try {
            List<String> command = new ArrayList<>();
            command.addAll(nativeImage);
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
                additionalBuildArgs.forEach(command::add);
            }
            command.add("-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
            command.add("-jar");
            command.add(runnerJarName);
            //https://github.com/oracle/graal/issues/660
            command.add("-J-Djava.util.concurrent.ForkJoinPool.common.parallelism=1");
            if (reportErrorsAtRuntime) {
                command.add("-H:+ReportUnsupportedElementsAtRuntime");
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
            if (!enableServer) {
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
        if (vmName.contains("-rc9") || vmName.contains("-rc10") || vmName.contains("-rc11")) {
            log.error("Out of date RC build of GraalVM detected! Please upgrade to RC12");
            return true;
        }
        return false;
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
                    default:
                        return false;
                }
                return true;
            }
        };
    }
}
