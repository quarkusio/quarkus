package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.ArtifactTypeUtil.isJLink;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ServiceLoader;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultJLinkLauncher;
import io.quarkus.test.common.JLinkLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.quarkus.test.junit.common.JdkUtil;
import io.smallrye.config.Config;

public class JLinkLauncherProvider implements ArtifactLauncherProvider {

    @Override
    public boolean supportsArtifactType(String type, String testProfile) {
        return isJLink(type);
    }

    @Override
    public JLinkLauncher create(CreateContext context) {
        String pathStr = context.quarkusArtifactProperties().getProperty("path");
        if (pathStr == null || pathStr.isEmpty()) {
            throw new IllegalStateException("The path of the jlink image could not be determined");
        }

        String launcherName = context.quarkusArtifactProperties()
                .getProperty("metadata.launcher-name", "my-app");

        Path imagePath = context.buildOutputDirectory().resolve(pathStr);

        JLinkLauncher launcher;
        ServiceLoader<JLinkLauncher> loader = ServiceLoader.load(JLinkLauncher.class);
        Iterator<JLinkLauncher> iterator = loader.iterator();
        if (iterator.hasNext()) {
            launcher = iterator.next();
        } else {
            launcher = new DefaultJLinkLauncher();
        }

        Config config = Config.get();
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);

        boolean aotEnabled = config.getOptionalValue("quarkus.package.jar.aot.enabled", Boolean.class)
                .orElse(Boolean.FALSE)
                && (context.profile() == null);

        List<String> additionalRecordingArgs = config
                .getOptionalValues("quarkus.package.jar.aot.additional-recording-args", String.class)
                .orElse(List.of());

        List<String> recordingArgs;
        List<String> postCloseCommand;
        Optional<Path> aotResultPath;
        String aotResultDescription;

        if (aotEnabled) {
            RecordingConfig rc = buildRecordingConfig(imagePath, additionalRecordingArgs);
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

        launcher.init(new DefaultJLinkInitContext(
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                testConfig.waitTime(),
                config.getOptionalValue("quarkus.shutdown.timeout", Duration.class).orElse(Duration.ZERO),
                testConfig.integrationTestProfile(),
                TestConfigUtil.argLineValues(testConfig.argLine().orElse("")),
                testConfig.env(),
                context.devServicesLaunchResult(),
                imagePath,
                launcherName,
                recordingArgs,
                postCloseCommand,
                aotResultPath,
                aotResultDescription));
        return launcher;
    }

    private record RecordingConfig(List<String> recordingArgs, List<String> postCloseCommand, Optional<Path> aotResultPath,
            String aotResultDescription) {
    }

    private static RecordingConfig buildRecordingConfig(Path imagePath, List<String> additionalRecordingArgs) {
        if (JdkUtil.isSemeru()) {
            Path sccDir = imagePath.resolveSibling("app-scc");
            List<String> recordingArgs = new ArrayList<>();
            recordingArgs.add("-Xshareclasses:name=quarkus-app,cacheDir=" + sccDir);
            recordingArgs.addAll(additionalRecordingArgs);
            return new RecordingConfig(recordingArgs, List.of(), Optional.of(sccDir), "SCC cache");
        }

        Path aotConf = imagePath.resolveSibling("app.aotconf");
        Path aotFile = imagePath.resolveSibling("app.aot");

        List<String> recordingArgs = new ArrayList<>();
        recordingArgs.add("-XX:AOTMode=record");
        recordingArgs.add("-XX:AOTConfiguration=%s".formatted(aotConf));
        recordingArgs.addAll(additionalRecordingArgs);

        List<String> postCloseCmd = new ArrayList<>();
        postCloseCmd.add("-XX:AOTMode=create");
        postCloseCmd.add("-XX:AOTConfiguration=%s".formatted(aotConf));
        postCloseCmd.add("-XX:AOTCache=%s".formatted(aotFile));
        postCloseCmd.addAll(additionalRecordingArgs);

        return new RecordingConfig(recordingArgs, postCloseCmd, Optional.of(aotFile), "AOT file");
    }

    static class DefaultJLinkInitContext extends DefaultInitContextBase
            implements JLinkLauncher.JLinkInitContext {

        private final Path imagePath;
        private final String launcherName;
        private final List<String> recordingArgs;
        private final List<String> postCloseCommand;
        private final Optional<Path> aotResultPath;
        private final String aotResultDescription;

        DefaultJLinkInitContext(int httpPort, int httpsPort, Duration waitTime, Duration shutdownTimeout,
                String testProfile,
                List<String> argLine, Map<String, String> env,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult,
                Path imagePath, String launcherName,
                List<String> recordingArgs, List<String> postCloseCommand,
                Optional<Path> aotResultPath, String aotResultDescription) {
            super(httpPort, httpsPort, waitTime, shutdownTimeout, testProfile, argLine, env, devServicesLaunchResult);
            this.imagePath = imagePath;
            this.launcherName = launcherName;
            this.recordingArgs = recordingArgs;
            this.postCloseCommand = postCloseCommand;
            this.aotResultPath = aotResultPath;
            this.aotResultDescription = aotResultDescription;
        }

        @Override
        public Path imagePath() {
            return imagePath;
        }

        @Override
        public String launcherName() {
            return launcherName;
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
    }
}
