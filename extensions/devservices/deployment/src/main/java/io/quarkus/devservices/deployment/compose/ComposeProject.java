package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.common.Labels.*;
import static java.lang.Boolean.TRUE;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ResourceReaper;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Network;

import io.quarkus.devservices.common.ContainerUtil;
import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.runtime.util.StringUtil;

/**
 * A wrapper around compose that starts and stops services defined a set of compose files.
 */
public class ComposeProject {

    private static final Logger LOG = Logger.getLogger(ComposeProject.class);
    public static final String DEFAULT_NETWORK_NAME = "default";

    private final DockerClient dockerClient;
    private final ComposeFiles composeFiles;
    private final String project;
    private final String executable;

    private final Duration startupTimeout;
    private final Duration stopTimeout;
    private final boolean stopContainers;
    private final boolean ryukEnabled;
    private final boolean followContainerLogs;
    private final Boolean build;
    private final List<String> options;
    private final List<String> profiles;
    private final Map<String, Integer> scalingPreferences;
    private final Map<String, String> env;
    private final boolean removeVolumes;
    private final String removeImages;

    private final Map<String, WaitAllStrategy> waitStrategies;

    private List<ComposeServiceWaitStrategyTarget> serviceInstances;
    private List<Network> networks;

    public ComposeProject(DockerClient dockerClient,
            ComposeFiles composeFiles,
            String executable,
            String project,
            Duration startupTimeout,
            Duration stopTimeout,
            boolean stopContainers,
            boolean ryukEnabled,
            boolean followContainerLogs,
            boolean removeVolumes,
            String removeImages,
            Boolean build,
            List<String> options,
            List<String> profiles,
            Map<String, Integer> scalingPreferences,
            Map<String, String> env) {
        this.dockerClient = dockerClient;
        this.composeFiles = composeFiles;
        this.project = project;
        this.executable = executable;
        this.startupTimeout = startupTimeout;
        this.stopTimeout = stopTimeout;
        this.stopContainers = stopContainers;
        this.ryukEnabled = ryukEnabled;
        this.followContainerLogs = followContainerLogs;
        this.options = options;
        this.profiles = profiles;
        this.scalingPreferences = scalingPreferences;
        this.env = env;
        this.removeVolumes = removeVolumes;
        this.removeImages = removeImages;
        this.build = build;

        this.waitStrategies = new HashMap<>();
        registerWaitStrategies(composeFiles, waitStrategies);
    }

    /**
     * can have multiple wait strategies for a single container, e.g. if waiting on several ports
     * if no wait strategy is defined, the WaitAllStrategy will return immediately.
     * The WaitAllStrategy uses the startup timeout for everything as a global maximum, but we expect timeouts to be handled by
     * the inner strategies.
     */
    public void addWaitStrategy(Map<String, WaitAllStrategy> strategies, String instanceName, WaitStrategy strategy) {
        strategies.computeIfAbsent(instanceName,
                ignored -> new WaitAllStrategy(WaitAllStrategy.Mode.WITH_MAXIMUM_OUTER_TIMEOUT)
                        .withStartupTimeout(startupTimeout))
                .withStrategy(strategy);
        LOG.debugv("Added wait strategy {0} for service {1}", strategy, instanceName);
    }

    private void registerWaitStrategies(ComposeFiles composeFiles,
            Map<String, WaitAllStrategy> waitStrategies) {
        // Iterate over service definitions
        for (ComposeServiceDefinition definition : composeFiles.getServiceDefinitions().values()) {
            String serviceName = definition.getServiceName();
            Map<String, Object> labels = definition.getLabels();
            // Skip the service if profiles doesn't match
            if (!definition.getProfiles().isEmpty() &&
                    definition.getProfiles().stream().noneMatch(profiles::contains)) {
                continue;
            }
            // Add wait for health check
            if (definition.hasHealthCheck()) {
                addWaitStrategy(waitStrategies, serviceName, Wait.forHealthcheck());
            } else {
                for (Map.Entry<String, Object> e : labels.entrySet()) {
                    // Add wait for log message
                    if (e.getKey().startsWith(COMPOSE_WAIT_FOR_LOGS)) {
                        int times = 1;
                        if (e.getKey().length() > COMPOSE_WAIT_FOR_LOGS.length()) {
                            try {
                                times = Integer.parseInt(e.getKey().replace(COMPOSE_WAIT_FOR_LOGS + ".", ""));
                            } catch (NumberFormatException t) {
                                LOG.warnv("Cannot parse label `{}`", e.getKey());
                            }
                        }
                        addWaitStrategy(waitStrategies, serviceName, Wait.forLogMessage((String) e.getValue(), times));
                    }
                }
                // Add wait for port availability
                if (labels.get(COMPOSE_WAIT_FOR_PORTS_DISABLE) != TRUE) {
                    int[] ports = definition.getPorts().stream().mapToInt(ExposedPort::getPort).toArray();
                    String waitForTimeout = (String) labels.get(COMPOSE_WAIT_FOR_PORTS_TIMEOUT);
                    Duration timeout = waitForTimeout != null ? Duration.parse("PT" + waitForTimeout) : startupTimeout;
                    addWaitStrategy(waitStrategies, serviceName, Wait.forListeningPorts(ports).withStartupTimeout(timeout));
                }
            }
        }
    }

    /**
     * @return the project name
     */
    public String getProject() {
        return project;
    }

    // Visible for testing
    Map<String, WaitAllStrategy> getWaitStrategies() {
        return waitStrategies;
    }

    public synchronized void start() {
        registerContainersForShutdown();
        startServices();
        discoverServiceInstances(true);
    }

    public void waitUntilServicesReady(Executor waitOn) {
        checkServicesStarted();
        copyExposedPortsToContainers();
        CompletableFuture.allOf(serviceInstances.stream()
                .map(srv -> waitOnThread(srv, waitOn))
                .toArray(CompletableFuture[]::new))
                .join();
    }

    private void copyExposedPortsToContainers() {
        for (ComposeServiceWaitStrategyTarget instance : serviceInstances) {
            InspectContainerResponse inspectContainer = instance.get();
            Map<String, String> labels = inspectContainer.getConfig().getLabels();
            if (labels != null) {
                String exposedPortsPath = labels.get(COMPOSE_EXPOSED_PORTS);
                if (exposedPortsPath != null) {
                    String ports = inspectContainer.getNetworkSettings().getPorts().getBindings().entrySet().stream()
                            .filter(e -> e.getValue() != null)
                            .flatMap(e -> Arrays.stream(e.getValue())
                                    .map(c -> String.format("PORT_%d=%s", e.getKey().getPort(), c.getHostPortSpec())))
                            .collect(Collectors.joining("\n", "", "\n"));
                    if (!StringUtil.isNullOrEmpty(ports)) {
                        instance.copyFileToContainer(Transferable.of(ports.getBytes(StandardCharsets.UTF_8)),
                                exposedPortsPath);
                    }
                }
            }
        }
    }

    public void startAndWaitUntilServicesReady(Executor waitOn) {
        start();
        waitUntilServicesReady(waitOn);
    }

    private void checkServicesStarted() {
        if (serviceInstances == null || serviceInstances.isEmpty()) {
            throw new IllegalStateException("Services have not been started yet");
        }
    }

    private CompletableFuture<Void> waitOnThread(ComposeServiceWaitStrategyTarget instance, Executor waitOn) {
        if (waitOn == null) {
            return CompletableFuture.runAsync(() -> waitUntilReady(instance));
        } else {
            return CompletableFuture.runAsync(() -> waitUntilReady(instance), waitOn);
        }
    }

    private void waitUntilReady(ComposeServiceWaitStrategyTarget instance) {
        String serviceName = instance.getServiceName();
        final WaitStrategy strategy = waitStrategies.get(serviceName);
        if (strategy != null) {
            LOG.infov("Waiting for service {0} to be ready", serviceName);
            try {
                strategy.waitUntilReady(instance);
            } catch (Exception e) {
                LOG.infov("Service {0} not ready, logs: {1}", serviceName, instance.getLogs());
                throw e;
            }
            LOG.infov("Service {0} is ready", serviceName);
        }
    }

    private void registerContainersForShutdown() {
        if (ryukEnabled) {
            ResourceReaper
                    .instance()
                    .registerLabelsFilterForCleanup(Collections.singletonMap(DOCKER_COMPOSE_PROJECT, project));
        }
    }

    private void startServices() {
        // scaling for the services
        final String scalingOptions = scalingPreferences
                .entrySet()
                .stream()
                .map(entry -> "--scale " + entry.getKey() + "=" + entry.getValue())
                .distinct()
                .collect(Collectors.joining(" "));

        String command = getUpCommand(getOptions());

        if (build != null) {
            if (build) {
                command += " --build";
            } else {
                command += " --no-build";
            }
        }

        if (!StringUtil.isNullOrEmpty(scalingOptions)) {
            command += " " + scalingOptions;
        }

        // Run the docker compose container, which starts up the services
        runWithCompose(command, env);
    }

    public synchronized void discoverServiceInstances(boolean checkForRequiredServices) {
        Set<String> servicesToWaitFor = new HashSet<>(waitStrategies.keySet());
        List<ComposeServiceWaitStrategyTarget> serviceInstances = new ArrayList<>();
        for (Container container : listChildContainers()) {
            String state = container.getState();
            if ("running".equalsIgnoreCase(state) || "restarting".equalsIgnoreCase(state)) {
                ComposeServiceWaitStrategyTarget instance = createServiceInstance(container, followContainerLogs);
                serviceInstances.add(instance);
                servicesToWaitFor.remove(instance.getServiceName());
            }
        }

        if (checkForRequiredServices && !servicesToWaitFor.isEmpty()) {
            throw new IllegalStateException("Services named " + servicesToWaitFor +
                    " do not exist, but wait conditions have been defined for them.");
        }

        this.networks = listChildNetworks();
        this.serviceInstances = serviceInstances;
    }

    private List<Container> listChildContainers() {
        return dockerClient
                .listContainersCmd()
                .withLabelFilter(Map.of(DOCKER_COMPOSE_PROJECT, project))
                .withShowAll(true)
                .exec();
    }

    private List<Network> listChildNetworks() {
        return dockerClient
                .listNetworksCmd()
                .withFilter("label", List.of(DOCKER_COMPOSE_PROJECT + "=" + project))
                .exec();
    }

    private ComposeServiceWaitStrategyTarget createServiceInstance(Container container, boolean tailChildContainers) {
        var containerInstance = new ComposeServiceWaitStrategyTarget(dockerClient, container);
        if (tailChildContainers) {
            String containerId = containerInstance.getContainerId();
            String serviceName = containerInstance.getContainerName();
            followLogs(containerId, new JBossLoggingConsumer(LOG)
                    .withPrefix(serviceName)
                    .withSeparateOutputStreams());
        }
        return containerInstance;
    }

    private void followLogs(String containerId, Consumer<OutputFrame> consumer) {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.addConsumer(OutputFrame.OutputType.STDERR, consumer);
        dockerClient.logContainerCmd(containerId)
                .withFollowStream(true)
                .withStdErr(true)
                .withStdOut(true)
                .withSince(0)
                .exec(callback);
    }

    /**
     * Stops the services defined in the docker-compose file.
     */
    public synchronized void stop() {
        if (!stopContainers) {
            LOG.infov("Skipping compose down for project {0}", project);
            return;
        }
        String cmd = getDownCommand(getOptions());

        if (removeVolumes) {
            cmd += " -v";
        }
        // rmi option not available in podman-compose
        if (!isExecutablePodman() && !StringUtil.isNullOrEmpty(removeImages)) {
            cmd += " --rmi " + removeImages;
        }
        // Set the timeout for stopping the services
        cmd += " -t " + stopTimeout.getSeconds();
        // Run the docker compose container, which stops the services
        try {
            runWithCompose(cmd, env);
        } finally {
            this.networks = null;
            this.serviceInstances = null;
        }
    }

    private boolean isExecutablePodman() {
        return executable.contains("podman");
    }

    private String getUpCommand(String options) {
        return StringUtil.isNullOrEmpty(options) ? "compose up -d" : String.format("compose %s up -d", options);
    }

    private String getDownCommand(String options) {
        return StringUtil.isNullOrEmpty(options) ? "compose down" : String.format("compose %s down", options);
    }

    private String getOptions() {
        return String.join(" ", this.options);
    }

    public void runWithCompose(String cmd, Map<String, String> env) {
        new ComposeRunner(executable, composeFiles.getFiles(), project)
                .withCommand(cmd)
                .withEnv(env)
                .withProfiles(profiles)
                .run();
    }

    public List<ComposeServiceWaitStrategyTarget> getServices() {
        return serviceInstances;
    }

    public Map<String, String> getEnvVarConfig() {
        checkServicesStarted();
        return ContainerUtil.getEnvVarConfig(serviceInstances, ComposeProject::getEnvVarMappings);
    }

    public Map<String, String> getExposedPortConfig() {
        checkServicesStarted();
        return ContainerUtil.getPortConfig(serviceInstances, ComposeProject::getHostPortMappings);
    }

    private static Map<Integer, String> getHostPortMappings(InspectContainerResponse containerInfo) {
        Map<String, String> labels = containerInfo.getConfig().getLabels();
        if (labels == null) {
            return Collections.emptyMap();
        }
        return labels.entrySet().stream()
                .filter(e -> e.getKey().startsWith(COMPOSE_CONFIG_MAP_PORT)
                        && e.getKey().length() > COMPOSE_CONFIG_MAP_PORT.length() + 1
                        && !StringUtil.isNullOrEmpty(e.getValue()))
                .collect(Collectors.toMap(ComposeProject::getContainerPort, Map.Entry::getValue));
    }

    private static int getContainerPort(Map.Entry<String, String> e) {
        return Integer.parseInt(e.getKey().substring(COMPOSE_CONFIG_MAP_PORT.length() + 1));
    }

    private static Map<String, String> getEnvVarMappings(InspectContainerResponse containerInfo) {
        Map<String, String> labels = containerInfo.getConfig().getLabels();
        if (labels == null) {
            return Collections.emptyMap();
        }
        return labels.entrySet().stream()
                .filter(e -> e.getKey().startsWith(COMPOSE_CONFIG_MAP_ENV_VAR)
                        && e.getKey().length() > COMPOSE_CONFIG_MAP_ENV_VAR.length() + 1)
                .collect(Collectors.toMap(ComposeProject::getVarName,
                        e -> StringUtil.isNullOrEmpty(e.getValue()) ? getVarName(e) : e.getValue()));
    }

    private static String getVarName(Map.Entry<String, String> e) {
        return e.getKey().substring(COMPOSE_CONFIG_MAP_ENV_VAR.length() + 1);
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public String getDefaultNetworkId() {
        return networks.stream()
                .filter(n -> DEFAULT_NETWORK_NAME.equals(n.getLabels().get(DOCKER_COMPOSE_NETWORK)))
                // multiple networks can have the default label, but only one can have containers
                .filter(n -> !n.getContainers().isEmpty())
                .findFirst()
                .map(Network::getId)
                // this is not an id, but a useful fallback
                .orElse(project + "_" + DEFAULT_NETWORK_NAME);
    }

    public static class Builder {

        private final ComposeFiles files;
        private final String executable;
        private DockerClient dockerClient = DockerClientFactory.lazyClient();
        private String project;
        private Duration startupTimeout = Duration.ofMinutes(1);
        private Duration stopTimeout;
        private boolean stopContainers = true;
        private boolean ryukEnabled = true;
        private boolean followContainerLogs = false;
        private boolean removeVolumes = true;
        private Boolean build;
        private String removeImages;
        private List<String> options = Collections.emptyList();
        private List<String> profiles = Collections.emptyList();
        private Map<String, String> env = Collections.emptyMap();
        private Map<String, Integer> scalingPreferences = Collections.emptyMap();

        public Builder(ComposeFiles files, String executable) {
            this.files = files;
            this.project = files.getProjectName();
            this.executable = executable;
        }

        /**
         * Set the docker client to use for the compose project.
         *
         * @param dockerClient the docker client to use
         * @return this
         */
        public Builder withDockerClient(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
            return this;
        }

        /**
         * Set whether to stop containers when the project is stopped.
         *
         * @param stopContainers whether to stop containers when the project is stopped
         * @return this
         */
        public Builder withStopContainers(boolean stopContainers) {
            this.stopContainers = stopContainers;
            return this;
        }

        /**
         * Set whether to stop containers when the project is stopped.
         *
         * @param ryukEnabled whether to stop containers when the project is stopped
         * @return this
         */
        public Builder withRyukEnabled(boolean ryukEnabled) {
            this.ryukEnabled = ryukEnabled;
            return this;
        }

        /**
         * Set the startup timeout for the compose project.
         *
         * @param duration the startup timeout
         * @return this
         */
        public Builder withStartupTimeout(Duration duration) {
            this.startupTimeout = duration;
            return this;
        }

        /**
         * Set the stop timeout for the compose project.
         *
         * @param duration the startup timeout
         * @return this
         */
        public Builder withStopTimeout(Duration duration) {
            this.stopTimeout = duration;
            return this;
        }

        /**
         * Set whether to build the images before starting the compose project.
         *
         * @param build whether to build the images before starting the compose project
         * @return this
         */
        public Builder withBuild(Boolean build) {
            this.build = build;
            return this;
        }

        /**
         * Set environment variables to pass to the compose command.
         *
         * @param envVariables the environment variables to pass to the compose command
         * @return this
         */
        public Builder withEnv(Map<String, String> envVariables) {
            this.env = envVariables;
            return this;
        }

        /**
         * Set the options to pass to the compose command.
         *
         * @param options the options to pass to the compose command
         * @return this
         */
        public Builder withOptions(List<String> options) {
            this.options = Collections.unmodifiableList(options);
            return this;
        }

        /**
         * Set the profiles for the compose project.
         *
         * @param profiles the profiles for the compose project
         * @return this
         */
        public Builder withProfiles(List<String> profiles) {
            this.profiles = Collections.unmodifiableList(profiles);
            return this;
        }

        /**
         * Set the scaling preferences for the compose project.
         *
         * @param scalingPreferences the scaling preferences for the compose project
         * @return this
         */
        public Builder withScalingPreferences(Map<String, Integer> scalingPreferences) {
            this.scalingPreferences = Collections.unmodifiableMap(scalingPreferences);
            return this;
        }

        /**
         * Set whether to follow the container logs.
         *
         * @param followContainerLogs whether to follow the container logs
         * @return this
         */
        public Builder withFollowContainerLogs(boolean followContainerLogs) {
            this.followContainerLogs = followContainerLogs;
            return this;
        }

        /**
         * Remove images after containers shutdown.
         *
         * @return this instance, for chaining
         */
        public Builder withRemoveImages(String removeImages) {
            this.removeImages = removeImages;
            return this;
        }

        /**
         * Remove volumes after containers shut down.
         *
         * @param removeVolumes whether volumes are to be removed.
         * @return this instance, for chaining.
         */
        public Builder withRemoveVolumes(boolean removeVolumes) {
            this.removeVolumes = removeVolumes;
            return this;
        }

        /**
         * Set the project name for the compose project.
         *
         * @param project the project name for the compose project
         * @return this
         */
        public Builder withProject(String project) {
            this.project = project;
            return this;
        }

        public ComposeProject build() {
            return new ComposeProject(dockerClient, files,
                    executable,
                    project,
                    startupTimeout,
                    stopTimeout,
                    stopContainers,
                    ryukEnabled,
                    followContainerLogs,
                    removeVolumes,
                    removeImages,
                    build,
                    options,
                    profiles,
                    scalingPreferences,
                    env);
        }
    }

}
