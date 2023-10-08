package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_PROJECT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.ContainerRuntimeStatusBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.ComposeBuildTimeConfig;
import io.quarkus.deployment.dev.devservices.ComposeDevServicesBuildTimeConfig;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.devservices.common.ContainerUtil;
import io.smallrye.mutiny.unchecked.Unchecked;

/**
 * Processor that starts the Compose dev services.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class ComposeDevServicesProcessor {

    private static final Logger log = Logger.getLogger(ComposeDevServicesProcessor.class);

    static final String PROJECT_PREFIX = "quarkus-compose-devservice";
    static final Pattern COMPOSE_FILE = Pattern.compile("(^docker-compose|^compose).*.(yml|yaml)");

    static volatile ComposeRunningService runningCompose;
    static volatile ComposeDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.COMPOSE);
    }

    @BuildStep
    public void watchComposeFiles(ComposeBuildTimeConfig composeBuildTimeConfig,
            BuildProducer<HotDeploymentWatchedFileBuildItem> producer) {
        if (composeBuildTimeConfig.devservices().enabled()) {
            composeBuildTimeConfig.devservices().files().ifPresentOrElse(files -> {
                for (File file : filesFromConfigList(files)) {
                    producer.produce(new HotDeploymentWatchedFileBuildItem(file.getAbsolutePath()));
                }
            }, () -> producer.produce(HotDeploymentWatchedFileBuildItem.builder()
                    .setLocationPredicate(COMPOSE_FILE.asMatchPredicate())
                    .build()));
        }
    }

    @BuildStep
    public DevServicesComposeProjectBuildItem config(
            Executor buildExecutor,
            ComposeBuildTimeConfig composeBuildTimeConfig,
            ApplicationInfoBuildItem appInfo,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig,
            DockerStatusBuildItem dockerStatusBuildItem) throws IOException {

        ComposeDevServiceCfg configuration = new ComposeDevServiceCfg(composeBuildTimeConfig.devservices());

        if (runningCompose != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return runningCompose.toComposeBuildItem();
            }
            try {
                runningCompose.close();
            } finally {
                runningCompose = null;
                cfg = null;
            }
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Compose Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem, s -> s.getName().startsWith("ducttape")
                        || s.getName().equals("Process stdout") || s.getName().startsWith("build-"));
        try {
            runningCompose = startCompose(buildExecutor, configuration, appInfo.getName(),
                    dockerStatusBuildItem, devServicesConfig.timeout());
            if (runningCompose == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (RuntimeException e) {
            compressor.closeAndDumpCaptured();
            throw e;
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (runningCompose == null) {
            return new DevServicesComposeProjectBuildItem();
        }

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (runningCompose != null) {
                    try {
                        runningCompose.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                first = true;
                runningCompose = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        return runningCompose.toComposeBuildItem();
    }

    @BuildStep
    public DevServicesResultBuildItem toDevServicesResult(DevServicesComposeProjectBuildItem composeBuildItem) {
        if (composeBuildItem.getProject() != null) {
            return new DevServicesResultBuildItem(
                    "Compose Dev Services",
                    String.format("Project: %s, Services: %s", composeBuildItem.getProject(),
                            String.join(", ", composeBuildItem.getComposeServices().keySet())),
                    null,
                    composeBuildItem.getConfig());
        }
        return null;
    }

    private ComposeRunningService startCompose(Executor buildExecutor, ComposeDevServiceCfg cfg,
            String appName,
            ContainerRuntimeStatusBuildItem dockerStatusBuildItem,
            Optional<Duration> timeout) {
        if (!cfg.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Compose dev services, as it has been disabled in the config.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Docker isn't working, not starting Compose dev services.");
            return null;
        }

        ComposeProject.Builder builder = new ComposeProject.Builder(cfg.files, getComposeExecutable())
                .withIdentifier(PROJECT_PREFIX + "-" + appName)
                .withProject(cfg.project)
                .withEnv(cfg.envVariables)
                .withStopContainers(cfg.stopContainers)
                .withRyukEnabled(cfg.ryukEnabled)
                .withProfiles(cfg.profiles)
                .withOptions(cfg.options)
                .withRemoveImages(cfg.removeImages)
                .withRemoveVolumes(cfg.removeVolumes)
                .withFollowContainerLogs(cfg.followContainerLogs)
                .withScalingPreferences(cfg.scalingPreferences)
                .withBuild(cfg.build);

        timeout.ifPresent(builder::withStartupTimeout);
        ComposeProject compose = builder.build();

        if (cfg.files.isEmpty()) {
            if (cfg.project == null) {
                // compose no files found
                log.debug("Could not find any compose files, not starting Compose dev services.");
                return null;
            } else {
                // discover existing services
                compose.discoverServiceInstances();
                if (!compose.getServices().isEmpty()) {
                    return new ComposeRunningService(compose, false);
                }
            }
        }

        // Start compose
        compose.startAndWaitUntilServicesReady(buildExecutor);
        return new ComposeRunningService(compose, true);
    }

    private static class ComposeRunningService extends RunningDevService {

        private final Map<String, List<RunningContainer>> composeServices;

        public ComposeRunningService(ComposeProject compose, boolean isOwner) {
            super(compose.getProject(), "compose", null, isOwner ? compose::stop : null, configs(compose));
            this.composeServices = composeServices(compose);
        }

        static Map<String, String> configs(ComposeProject compose) {
            Map<String, String> configs = new HashMap<>();
            configs.putAll(compose.getEnvVarConfig());
            configs.putAll(compose.getExposedPortConfig());
            configs.put(DOCKER_COMPOSE_PROJECT, compose.getProject());
            return configs;
        }

        static Map<String, List<RunningContainer>> composeServices(ComposeProject compose) {
            return compose.getServices().stream()
                    .collect(Collectors.groupingBy(ComposeServiceWaitStrategyTarget::getServiceName,
                            Collectors.mapping(s -> ContainerUtil.toRunningContainer(s.getContainerInfo()),
                                    Collectors.toList())));
        }

        public DevServicesComposeProjectBuildItem toComposeBuildItem() {
            return new DevServicesComposeProjectBuildItem(getName(), composeServices, getConfig());
        }
    }

    private String getComposeExecutable() {
        return ContainerRuntimeUtil.detectContainerRuntime().getExecutableName()
                + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");
    }

    static boolean isComposeFile(Path p) {
        return COMPOSE_FILE.matcher(p.getFileName().toString()).matches();
    }

    static List<File> filesFromConfigList(List<String> l) {
        return l.stream()
                .map(f -> Paths.get(f).toAbsolutePath().normalize())
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    static List<File> collectComposeFilesFromProjectRoot() throws RuntimeException {
        try (Stream<Path> list = Files.list(Paths.get(".").toAbsolutePath().normalize())) {
            return list
                    .filter(ComposeDevServicesProcessor::isComposeFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ComposeDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String project;
        private final boolean stopContainers;
        private final boolean ryukEnabled;
        private final List<File> files;
        private final List<String> profiles;
        private final List<String> options;
        private final String removeImages;
        private final Boolean build;
        private final boolean removeVolumes;
        private final boolean followContainerLogs;
        private final Map<String, String> envVariables;
        private final Map<String, Integer> scalingPreferences;
        private final String filesSha;

        public ComposeDevServiceCfg(ComposeDevServicesBuildTimeConfig cfg) {
            this.devServicesEnabled = cfg.enabled();
            this.files = cfg.files()
                    .map(ComposeDevServicesProcessor::filesFromConfigList)
                    .orElseGet(ComposeDevServicesProcessor::collectComposeFilesFromProjectRoot);
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            Base64.Encoder encoder = Base64.getEncoder();
            this.filesSha = this.files.stream().sorted()
                    .map(Unchecked.function(f -> Files.readAllBytes(f.toPath())))
                    .map(md::digest)
                    .map(encoder::encodeToString)
                    .collect(Collectors.joining());
            this.project = cfg.projectName().orElse(null);
            this.stopContainers = cfg.stopContainers();
            this.ryukEnabled = cfg.ryukEnabled();
            this.profiles = cfg.profiles().orElse(Collections.emptyList());
            this.options = cfg.options().orElse(Collections.emptyList());
            this.removeImages = cfg.removeImages().map(Enum::name).map(String::toLowerCase).orElse(null);
            this.removeVolumes = cfg.removeVolumes();
            this.envVariables = cfg.envVariables();
            this.scalingPreferences = cfg.scale();
            this.followContainerLogs = cfg.followContainerLogs();
            this.build = cfg.build().orElse(null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ComposeDevServiceCfg that = (ComposeDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(project, that.project)
                    && Objects.equals(stopContainers, that.stopContainers)
                    && Objects.equals(ryukEnabled, that.ryukEnabled)
                    && Objects.equals(files, that.files)
                    && Objects.equals(filesSha, that.filesSha)
                    && Objects.equals(profiles, that.profiles)
                    && Objects.equals(options, that.options)
                    && Objects.equals(removeImages, that.removeImages)
                    && Objects.equals(removeVolumes, that.removeVolumes)
                    && Objects.equals(followContainerLogs, that.followContainerLogs)
                    && Objects.equals(build, that.build)
                    && Objects.equals(envVariables, that.envVariables)
                    && Objects.equals(scalingPreferences, that.scalingPreferences);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, project, stopContainers, ryukEnabled, files, filesSha,
                    profiles, options, removeImages, removeVolumes,
                    followContainerLogs, build, envVariables, scalingPreferences);
        }
    }
}
