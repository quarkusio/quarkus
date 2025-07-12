package io.quarkus.devservices.deployment;

import static io.quarkus.deployment.dev.testing.MessageFormat.BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_UNDERLINE;
import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.MessageFormat.UNDERLINE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetworkSettings;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ApplicationInstanceIdBuildItem;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devservice.runtime.config.DevServicesConfigBuilder;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerUtil;
import io.quarkus.devservices.common.Labels;
import io.quarkus.devservices.common.StartableContainer;
import io.quarkus.devservices.crossclassloader.runtime.RunningService;
import io.quarkus.devui.spi.buildtime.FooterLogBuildItem;

public class DevServicesProcessor {

    private static final String EXEC_FORMAT = "%s exec -it %s /bin/bash";

    static volatile ConsoleStateManager.ConsoleContext context;
    static volatile boolean logForwardEnabled = false;
    static Set<ContainerLogForwarder> containerLogForwarders = new HashSet<>();

    @BuildStep
    public DevServicesNetworkIdBuildItem networkId(
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            Optional<DevServicesComposeProjectBuildItem> composeProjectBuildItem) {
        String networkId = composeProjectBuildItem
                .map(DevServicesComposeProjectBuildItem::getDefaultNetworkId)
                .or(() -> devServicesLauncherConfig.flatMap(ignored -> getSharedNetworkId()))
                .orElse(null);
        return new DevServicesNetworkIdBuildItem(networkId);
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Produce(ServiceStartBuildItem.class)
    public DevServicesCustomizerBuildItem containerCustomizer(LaunchModeBuildItem launchMode,
            DevServicesConfig globalDevServicesConfig) {
        return new DevServicesCustomizerBuildItem((devService, startable) -> {
            if (startable instanceof StartableContainer startableContainer) {
                GenericContainer<?> container = startableContainer.getContainer();
                ConfigureUtil.configureLabels(container, launchMode.getLaunchMode());
                container.withLabel(Labels.QUARKUS_DEV_SERVICE, devService.getServiceName());
                globalDevServicesConfig.timeout().ifPresent(container::withStartupTimeout);
            } else if (startable instanceof GenericContainer container) {
                ConfigureUtil.configureLabels(container, launchMode.getLaunchMode());
                globalDevServicesConfig.timeout().ifPresent(container::withStartupTimeout);
                container.withLabel(Labels.QUARKUS_DEV_SERVICE, devService.getServiceName());
            }
            return startable;
        });
    }

    /**
     * Get the network id from the shared testcontainers network, without forcing the creation of the network.
     *
     * @return the network id if available, empty otherwise
     */
    private Optional<String> getSharedNetworkId() {
        try {
            Field id;
            Object sharedNetwork;
            var tccl = Thread.currentThread().getContextClassLoader();
            if (tccl.getName().contains("Deployment")) {
                Class<?> networkClass = tccl.getParent().loadClass("org.testcontainers.containers.Network");
                sharedNetwork = networkClass.getField("SHARED").get(null);
                Class<?> networkImplClass = tccl.getParent().loadClass("org.testcontainers.containers.Network$NetworkImpl");
                id = networkImplClass.getDeclaredField("id");
            } else {
                sharedNetwork = Network.SHARED;
                id = Network.NetworkImpl.class.getDeclaredField("id");
            }
            id.setAccessible(true);
            String value = (String) id.get(sharedNetwork);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Produce(ServiceStartBuildItem.class)
    DevServicesRegistryBuildItem devServicesRegistry(LaunchModeBuildItem launchMode,
            ApplicationInstanceIdBuildItem applicationId,
            DevServicesConfig globalDevServicesConfig,
            CuratedApplicationShutdownBuildItem shutdownBuildItem) {
        DevServicesRegistryBuildItem registryBuildItem = new DevServicesRegistryBuildItem(applicationId.getUUID(),
                globalDevServicesConfig, launchMode.getLaunchMode());
        shutdownBuildItem.addCloseTask(registryBuildItem::closeAllRunningServices, true);
        return registryBuildItem;
    }

    @BuildStep
    public RunTimeConfigBuilderBuildItem registerDevResourcesConfigSource(
            List<DevServicesResultBuildItem> devServicesRequestBuildItems) {
        // Once all the dev services are registered, we can share config
        return new RunTimeConfigBuilderBuildItem(DevServicesConfigBuilder.class);
    }

    @BuildStep(onlyIf = { IsDevelopment.class })
    public List<DevServiceDescriptionBuildItem> config(
            DockerStatusBuildItem dockerStatusBuildItem,
            BuildProducer<ConsoleCommandBuildItem> commandBuildItemBuildProducer,
            BuildProducer<FooterLogBuildItem> footerLogProducer,
            LaunchModeBuildItem launchModeBuildItem,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            List<DevServicesResultBuildItem> devServicesResults,
            DevServicesRegistryBuildItem devServicesRegistry) {
        containerLogForwarders.clear();
        boolean isContainerRuntimeAvailable = dockerStatusBuildItem.isContainerRuntimeAvailable();
        List<DevServiceDescriptionBuildItem> serviceDescriptions = buildServiceDescriptions(
                isContainerRuntimeAvailable, devServicesResults, devServicesRegistry,
                devServicesLauncherConfig);

        for (DevServiceDescriptionBuildItem devService : serviceDescriptions) {

            containerLogForwarders.add(new ContainerLogForwarder(devService));

        }

        // Build commands if we are in local dev mode
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return serviceDescriptions;
        }

        commandBuildItemBuildProducer.produce(
                new ConsoleCommandBuildItem(new DevServicesCommand(serviceDescriptions)));

        // Dev UI Log stream
        for (DevServiceDescriptionBuildItem service : serviceDescriptions) {

            footerLogProducer.produce(new FooterLogBuildItem(service.getName(), () -> {
                ContainerInfo containerInfo = service.getContainerInfo();
                return createLogPublisher(containerInfo);
            }));
        }

        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("Dev Services");
        }
        context.reset(
                new ConsoleCommand('c', "Show Dev Services containers", null, () -> {
                    List<DevServiceDescriptionBuildItem> descriptions = buildServiceDescriptions(
                            isContainerRuntimeAvailable, devServicesResults, devServicesRegistry,
                            devServicesLauncherConfig);
                    StringBuilder builder = new StringBuilder();
                    builder.append("\n\n")
                            .append(RED + "==" + RESET + " " + UNDERLINE + "Dev Services" + NO_UNDERLINE)
                            .append("\n\n");
                    for (DevServiceDescriptionBuildItem devService : descriptions) {
                        printDevService(builder, devService, true);
                        builder.append("\n");
                    }
                    System.out.println(builder);
                }),
                new ConsoleCommand('g', "Follow Dev Services logs in the console",
                        new ConsoleCommand.HelpState(() -> logForwardEnabled ? GREEN : RED,
                                () -> logForwardEnabled ? "enabled" : "disabled"),
                        this::toggleLogForwarders));
        return serviceDescriptions;
    }

    private Flow.Publisher<String> createLogPublisher(ContainerInfo containerInfo) {

        try (FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback()) {
            SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
            resultCallback.addConsumer(OutputFrame.OutputType.STDERR,
                    frame -> publisher.submit(frame.getUtf8String()));
            resultCallback.addConsumer(OutputFrame.OutputType.STDOUT,
                    frame -> publisher.submit(frame.getUtf8String()));
            if (containerInfo != null) {
                String containerId = containerInfo.id();
                LogContainerCmd logCmd = DockerClientFactory.lazyClient()
                        .logContainerCmd(containerId)
                        .withFollowStream(true)
                        .withTailAll()
                        .withStdErr(true)
                        .withStdOut(true);
                logCmd.exec(resultCallback);
            }

            return publisher;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<DevServiceDescriptionBuildItem> buildServiceDescriptions(
            boolean isContainerRuntimeAvailable,
            List<DevServicesResultBuildItem> devServicesResults,
            DevServicesRegistryBuildItem devServicesRegistry,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {

        // Build descriptions
        Set<String> configKeysFromDevServices = new HashSet<>();
        List<DevServiceDescriptionBuildItem> descriptions = new ArrayList<>();
        for (DevServicesResultBuildItem buildItem : devServicesResults) {
            configKeysFromDevServices.addAll(buildItem.getConfig().keySet());
            descriptions.add(toDevServiceDescription(buildItem, buildItem::getContainerId, isContainerRuntimeAvailable,
                    devServicesRegistry));
        }
        // Sort descriptions by name
        descriptions.sort(Comparator.comparing(DevServiceDescriptionBuildItem::getName));
        // Add description from other dev service configs as last
        if (devServicesLauncherConfig.isPresent()) {
            Map<String, String> config = new TreeMap<>(devServicesLauncherConfig.get().getConfig());
            for (String key : configKeysFromDevServices) {
                config.remove(key);
            }
            if (!config.isEmpty()) {
                descriptions.add(new DevServiceDescriptionBuildItem("Additional Dev Services config", config));
            }
        }
        return descriptions;
    }

    private Optional<Container> fetchContainerInfo(String containerId, boolean isContainerRuntimeAvailable) {
        if (containerId == null || !isContainerRuntimeAvailable) {
            return Optional.empty();
        }
        return DockerClientFactory.lazyClient().listContainersCmd()
                .withIdFilter(Collections.singleton(containerId))
                .withShowAll(true)
                .exec()
                .stream()
                .findAny();
    }

    private DevServiceDescriptionBuildItem toDevServiceDescription(DevServicesResultBuildItem buildItem,
            Supplier<String> containerIdFun,
            boolean isContainerRuntimeAvailable,
            DevServicesRegistryBuildItem devServicesRegistry) {
        if (!buildItem.isStartable()) {
            return new DevServiceDescriptionBuildItem(buildItem.getName(), buildItem.getDescription(),
                    toContainerInfo(containerIdFun, isContainerRuntimeAvailable), buildItem.getConfig());
        } else {
            return new DevServiceDescriptionBuildItem(buildItem.getName(),
                    buildItem.getDescription(),
                    toContainerInfo(() -> {
                        RunningService runningService = devServicesRegistry.getRunningServices(buildItem.getName(),
                                buildItem.getServiceName(), buildItem.getServiceConfig());
                        return runningService == null ? null : runningService.containerId();
                    }, isContainerRuntimeAvailable), () -> {
                        RunningService runningService = devServicesRegistry.getRunningServices(buildItem.getName(),
                                buildItem.getServiceName(), buildItem.getServiceConfig());
                        return runningService == null ? null : runningService.configs();
                    });
        }
    }

    private Supplier<ContainerInfo> toContainerInfo(Supplier<String> containerIdFun, boolean isContainerRuntimeAvailable) {

        return () -> {
            String containerId = containerIdFun.get();
            if (containerId != null) {
                Optional<Container> maybeContainer = fetchContainerInfo(containerId, isContainerRuntimeAvailable);
                if (maybeContainer.isPresent()) {
                    Container container = maybeContainer.get();
                    return new ContainerInfo(container.getId(), container.getNames(), container.getImage(),
                            container.getStatus(), getNetworks(container), container.getLabels(), getExposedPorts(container));
                }
            }
            return null;

        };

    }

    private static Map<String, String[]> getNetworks(Container container) {
        ContainerNetworkSettings networkSettings = container.getNetworkSettings();
        if (networkSettings == null) {
            return null;
        }
        return ContainerUtil.getNetworks(networkSettings.getNetworks());
    }

    private ContainerInfo.ContainerPort[] getExposedPorts(Container container) {
        return Arrays.stream(container.getPorts())
                .map(c -> new ContainerInfo.ContainerPort(c.getIp(), c.getPrivatePort(), c.getPublicPort(), c.getType()))
                .toArray(ContainerInfo.ContainerPort[]::new);
    }

    private synchronized void toggleLogForwarders() {
        if (logForwardEnabled) {
            for (ContainerLogForwarder logForwarder : containerLogForwarders) {
                if (logForwarder.isRunning()) {
                    logForwarder.close();
                }
            }
            logForwardEnabled = false;
        } else {
            for (ContainerLogForwarder logForwarder : containerLogForwarders) {
                logForwarder.start();
            }
            logForwardEnabled = true;
        }
    }

    public static void printDevService(StringBuilder builder, DevServiceDescriptionBuildItem devService, boolean withStatus) {
        builder.append(BOLD).append(devService.getName()).append(NO_BOLD);
        builder.append("\n");

        ContainerInfo containerInfo = devService.getContainerInfo();
        if (containerInfo != null) {
            builder.append(String.format("  %-18s", "Container: "))
                    .append(containerInfo.id(), 0, 12)
                    .append(containerInfo.formatNames())
                    .append("  ")
                    .append(containerInfo.imageName())
                    .append("\n");
            builder.append(String.format("  %-18s", "Network: "))
                    .append(containerInfo.formatNetworks())
                    .append(" - ")
                    .append(containerInfo.formatPorts())
                    .append("\n");

            ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime(false);
            if (containerRuntime != null) {
                builder.append(String.format("  %-18s", "Exec command: "))
                        .append(String.format(EXEC_FORMAT,
                                containerRuntime.getExecutableName(),
                                containerInfo.getShortId()))
                        .append("\n");
            }
        }

        if (!devService.getConfigs().isEmpty()) {
            builder.append(String.format("  %-18s", "Injected config: "));
            boolean indent = false;
            for (Entry<String, String> devServiceConfigEntry : devService.getConfigs().entrySet()) {
                if (indent) {
                    builder.append(String.format("  %-18s", " "));
                }
                builder.append(
                        String.format("- %s=%s\n",
                                devServiceConfigEntry.getKey(), devServiceConfigEntry.getValue()));
                indent = true;
            }
        }
    }

}
