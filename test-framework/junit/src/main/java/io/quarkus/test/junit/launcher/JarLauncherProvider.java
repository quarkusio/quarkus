package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultJarLauncher;
import io.quarkus.test.common.JarArtifactLauncher;
import io.quarkus.test.common.JvmStartupArchiveTraining;
import io.quarkus.test.common.JvmStartupArchiveTraining.ExecutionTarget;
import io.quarkus.test.common.TestConfigUtil;
import io.quarkus.test.junit.common.JdkUtil;
import io.smallrye.config.Config;

/**
 * Creates JAR launchers from Quarkus artifact metadata.
 * <p>
 * Explicit startup-archive metadata is accepted only for host-JVM training and is translated into recording,
 * optional post-close creation, and result-reporting values for the selected {@link JarArtifactLauncher}.
 */
public class JarLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        return isJar(type);
    }

    @Override
    public JarArtifactLauncher create(CreateContext context) {
        String pathStr = context.quarkusArtifactProperties().getProperty("path");
        if ((pathStr != null) && !pathStr.isEmpty()) {
            JarArtifactLauncher launcher;
            ServiceLoader<JarArtifactLauncher> loader = ServiceLoader.load(JarArtifactLauncher.class);
            Iterator<JarArtifactLauncher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                launcher = iterator.next();
            } else {
                launcher = new DefaultJarLauncher();
            }

            Config config = Config.get();
            TestConfig testConfig = config.getConfigMapping(TestConfig.class);

            Path jarPath = context.buildOutputDirectory().resolve(pathStr);
            Optional<JvmStartupArchiveTraining> startupArchiveTraining = JvmStartupArchiveTraining
                    .fromMetadata(context.quarkusArtifactProperties());

            boolean aotEnabled = config.getOptionalValue("quarkus.package.jar.aot.enabled", Boolean.class)
                    .or(() -> config.getOptionalValue("quarkus.package.jar.appcds.use-aot", Boolean.class))
                    .orElse(Boolean.FALSE)
                    // only record AOT file for the default profile
                    && (context.profile() == null);

            List<String> additionalRecordingArgs = config
                    .getOptionalValues("quarkus.package.jar.aot.additional-recording-args", String.class)
                    .orElse(List.of());

            List<String> recordingArgs;
            List<String> postCloseCommand;
            Optional<Path> aotResultPath;
            String aotResultDescription;

            if (startupArchiveTraining.isPresent()) {
                JvmStartupArchiveTraining training = startupArchiveTraining.get();
                if (training.executionTarget() != ExecutionTarget.HOST_JVM) {
                    throw new IllegalArgumentException("A JAR launcher requires HOST_JVM startup-archive training, not "
                            + training.executionTarget());
                }
                RecordingConfig rc = buildRecordingConfig(training, additionalRecordingArgs, isSemeruOrOpenJ9(),
                        Runtime.version().feature());
                recordingArgs = rc.recordingArgs();
                postCloseCommand = rc.postCloseCommand();
                aotResultPath = rc.aotResultPath();
                aotResultDescription = rc.aotResultDescription();
            } else if (aotEnabled) {
                RecordingConfig rc = buildRecordingConfig(jarPath, additionalRecordingArgs);
                recordingArgs = rc.recordingArgs();
                postCloseCommand = rc.postCloseCommand();
                aotResultPath = rc.aotResultPath();
                aotResultDescription = rc.aotResultDescription();
            } else {
                recordingArgs = List.of();
                postCloseCommand = List.of();
                aotResultPath = Optional.empty();
                aotResultDescription = "";
            }

            launcher.init(new DefaultJarInitContext(
                    config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                    config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                    testConfig.waitTime(),
                    config.getOptionalValue("quarkus.shutdown.timeout", Duration.class).orElse(Duration.ZERO),
                    testConfig.integrationTestProfile(),
                    TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                    testConfig.env(),
                    context.devServicesLaunchResult(),
                    jarPath,
                    recordingArgs,
                    postCloseCommand,
                    aotResultPath,
                    aotResultDescription,
                    startupArchiveTraining));
            return launcher;
        } else {
            throw new IllegalStateException("The path of the native binary could not be determined");
        }
    }

    record RecordingConfig(List<String> recordingArgs, List<String> postCloseCommand, Optional<Path> aotResultPath,
            String aotResultDescription) {
    }

    private static RecordingConfig buildRecordingConfig(Path jarPath, List<String> additionalRecordingArgs) {
        if (JdkUtil.isSemeru()) {
            Path sccDir = jarPath.resolveSibling("app-scc");
            List<String> recordingArgs = new ArrayList<>();
            recordingArgs.add("-Xshareclasses:name=quarkus-app,cacheDir=" + sccDir);
            recordingArgs.addAll(additionalRecordingArgs);
            return new RecordingConfig(recordingArgs, List.of(), Optional.of(sccDir), "SCC cache");
        }

        // Leyden AOT (HotSpot JDK 25+)
        Path aotConf = jarPath.resolveSibling("app.aotconf");
        Path aotFile = jarPath.resolveSibling("app.aot");

        List<String> recordingArgs = new ArrayList<>();
        recordingArgs.add("-XX:AOTMode=record");
        recordingArgs.add("-XX:AOTConfiguration=%s".formatted(aotConf));
        recordingArgs.addAll(additionalRecordingArgs);

        // Build the base command with AOT-specific flags only.
        // DefaultJarLauncher.runPostCloseCommand() prepends java binary and argLine,
        // then appends runtime system props, -jar, jar path, and program args.
        List<String> postCloseCmd = new ArrayList<>();
        postCloseCmd.add("-XX:AOTMode=create");
        postCloseCmd.add("-XX:AOTConfiguration=%s".formatted(aotConf));
        postCloseCmd.add("-XX:AOTCache=%s".formatted(aotFile));
        postCloseCmd.addAll(additionalRecordingArgs);

        return new RecordingConfig(recordingArgs, postCloseCmd, Optional.of(aotFile), "AOT file");
    }

    static RecordingConfig buildRecordingConfig(JvmStartupArchiveTraining training, List<String> additionalRecordingArgs,
            boolean semeruOrOpenJ9, int javaFeature) {
        return switch (training.type()) {
            case SCC -> {
                if (!semeruOrOpenJ9) {
                    throw new IllegalStateException(
                            "SCC startup-archive training requires an IBM Semeru/OpenJ9 test JVM");
                }
                List<String> recordingArgs = new ArrayList<>();
                recordingArgs.add("-Xshareclasses:name=quarkus-app,cacheDir=" + training.destination());
                recordingArgs.addAll(additionalRecordingArgs);
                yield new RecordingConfig(recordingArgs, List.of(), Optional.of(training.destination()), "SCC cache");
            }
            case AOT -> {
                if (semeruOrOpenJ9) {
                    throw new IllegalStateException(
                            "AOT startup-archive training requires a HotSpot/OpenJDK test JVM");
                }
                if (javaFeature < 25) {
                    throw new IllegalStateException(
                            "AOT startup-archive training requires a HotSpot/OpenJDK 25 or newer test JVM, but found Java "
                                    + javaFeature);
                }
                Path aotConfiguration = training.aotConfigurationDestination();
                List<String> recordingArgs = new ArrayList<>();
                recordingArgs.add("-XX:AOTMode=record");
                recordingArgs.add("-XX:AOTConfiguration=" + aotConfiguration);
                recordingArgs.addAll(additionalRecordingArgs);

                List<String> postCloseCommand = new ArrayList<>();
                postCloseCommand.add("-XX:AOTMode=create");
                postCloseCommand.add("-XX:AOTConfiguration=" + aotConfiguration);
                postCloseCommand.add(training.type().renderRuntimeOption(training.destination().toString()));
                postCloseCommand.addAll(additionalRecordingArgs);
                yield new RecordingConfig(recordingArgs, postCloseCommand, Optional.of(training.destination()), "AOT file");
            }
            case AppCDS -> throw new IllegalArgumentException(
                    "AppCDS is not supported by integration-test startup-archive training");
        };
    }

    private static boolean isSemeruOrOpenJ9() {
        return JdkUtil.isSemeru()
                || System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT).contains("openj9");
    }

    static class DefaultJarInitContext extends DefaultInitContextBase implements JarArtifactLauncher.JarInitContext {

        private final Path jarPath;
        private final List<String> recordingArgs;
        private final List<String> postCloseCommand;
        private final Optional<Path> aotResultPath;
        private final String aotResultDescription;
        private final Optional<JvmStartupArchiveTraining> startupArchiveTraining;

        DefaultJarInitContext(int httpPort, int httpsPort, Duration waitTime, Duration shutdownTimeout,
                String testProfile,
                List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult, Path jarPath,
                List<String> recordingArgs, List<String> postCloseCommand, Optional<Path> aotResultPath,
                String aotResultDescription, Optional<JvmStartupArchiveTraining> startupArchiveTraining) {
            super(httpPort, httpsPort, waitTime, shutdownTimeout, testProfile, argLine, env, devServicesLaunchResult);
            this.jarPath = jarPath;
            this.recordingArgs = recordingArgs;
            this.postCloseCommand = postCloseCommand;
            this.aotResultPath = aotResultPath;
            this.aotResultDescription = aotResultDescription;
            this.startupArchiveTraining = startupArchiveTraining;
        }

        @Override
        public Path jarPath() {
            return jarPath;
        }

        @Override
        public List<String> recordingArgs() {
            return recordingArgs;
        }

        @Override
        public List<String> postCloseCommand() {
            return postCloseCommand;
        }

        @Override
        public Optional<Path> aotResultPath() {
            return aotResultPath;
        }

        @Override
        public String aotResultDescription() {
            return aotResultDescription;
        }

        @Override
        public Optional<JvmStartupArchiveTraining> startupArchiveTraining() {
            return startupArchiveTraining;
        }
    }
}
