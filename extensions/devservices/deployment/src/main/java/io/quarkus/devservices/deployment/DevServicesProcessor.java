package io.quarkus.devservices.deployment;

import static io.quarkus.deployment.dev.testing.MessageFormat.BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_UNDERLINE;
import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.MessageFormat.UNDERLINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.spi.buildtime.FooterLogBuildItem;

public class DevServicesProcessor {

    private static final String EXEC_FORMAT = "%s exec -it %s /bin/bash";

    static volatile ConsoleStateManager.ConsoleContext context;
    static volatile boolean logForwardEnabled = false;
    static Map<String, ContainerLogForwarder> containerLogForwarders = new HashMap<>();

    @BuildStep(onlyIf = { IsDevelopment.class })
    public List<DevServiceDescriptionBuildItem> config(
            DockerStatusBuildItem dockerStatusBuildItem,
            BuildProducer<ConsoleCommandBuildItem> commandBuildItemBuildProducer,
            BuildProducer<FooterLogBuildItem> footerLogProducer,
            LaunchModeBuildItem launchModeBuildItem,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            List<DevServicesResultBuildItem> devServicesResults) {
        List<DevServiceDescriptionBuildItem> serviceDescriptions = buildServiceDescriptions(
                dockerStatusBuildItem, devServicesResults, devServicesLauncherConfig);

        for (DevServiceDescriptionBuildItem devService : serviceDescriptions) {
            if (devService.hasContainerInfo()) {
                containerLogForwarders.compute(devService.getContainerInfo().getId(),
                        (id, forwarder) -> Objects.requireNonNullElseGet(forwarder,
                                () -> new ContainerLogForwarder(devService)));
            }
        }

        // Build commands if we are in local dev mode
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return serviceDescriptions;
        }

        commandBuildItemBuildProducer.produce(
                new ConsoleCommandBuildItem(new DevServicesCommand(serviceDescriptions)));

        // Dev UI Log stream
        for (DevServiceDescriptionBuildItem service : serviceDescriptions) {
            if (service.getContainerInfo() != null) {
                footerLogProducer.produce(new FooterLogBuildItem(service.getName(), () -> {
                    return createLogPublisher(service.getContainerInfo().getId());
                }));
            }
        }

        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("Dev Services");
        }
        context.reset(
                new ConsoleCommand('c', "Show Dev Services containers", null, () -> {
                    List<DevServiceDescriptionBuildItem> descriptions = buildServiceDescriptions(
                            dockerStatusBuildItem, devServicesResults, devServicesLauncherConfig);
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

    private Flow.Publisher<String> createLogPublisher(String containerId) {
        try (FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback()) {
            SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
            resultCallback.addConsumer(OutputFrame.OutputType.STDERR,
                    frame -> publisher.submit(frame.getUtf8String()));
            resultCallback.addConsumer(OutputFrame.OutputType.STDOUT,
                    frame -> publisher.submit(frame.getUtf8String()));
            LogContainerCmd logCmd = DockerClientFactory.lazyClient()
                    .logContainerCmd(containerId)
                    .withFollowStream(true)
                    .withTailAll()
                    .withStdErr(true)
                    .withStdOut(true);
            logCmd.exec(resultCallback);

            return publisher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<DevServiceDescriptionBuildItem> buildServiceDescriptions(
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesResultBuildItem> devServicesResults,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {
        // Fetch container infos
        Set<String> containerIds = devServicesResults.stream()
                .map(DevServicesResultBuildItem::getContainerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Container> containerInfos = fetchContainerInfos(dockerStatusBuildItem, containerIds);
        // Build descriptions
        Set<String> configKeysFromDevServices = new HashSet<>();
        List<DevServiceDescriptionBuildItem> descriptions = new ArrayList<>();
        for (DevServicesResultBuildItem buildItem : devServicesResults) {
            configKeysFromDevServices.addAll(buildItem.getConfig().keySet());
            descriptions.add(toDevServiceDescription(buildItem, containerInfos.get(buildItem.getContainerId())));
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
                descriptions.add(new DevServiceDescriptionBuildItem("Additional Dev Services config", null, null, config));
            }
        }
        return descriptions;
    }

    private Map<String, Container> fetchContainerInfos(DockerStatusBuildItem dockerStatusBuildItem,
            Set<String> containerIds) {
        if (containerIds.isEmpty() || !dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            return Collections.emptyMap();
        }
        return DockerClientFactory.lazyClient().listContainersCmd()
                .withIdFilter(containerIds)
                .withShowAll(true)
                .exec()
                .stream()
                .collect(Collectors.toMap(Container::getId, Function.identity()));
    }

    private DevServiceDescriptionBuildItem toDevServiceDescription(DevServicesResultBuildItem buildItem, Container container) {
        if (container == null) {
            return new DevServiceDescriptionBuildItem(buildItem.getName(), buildItem.getDescription(), null,
                    buildItem.getConfig());
        } else {
            return new DevServiceDescriptionBuildItem(buildItem.getName(), buildItem.getDescription(),
                    toContainerInfo(container), buildItem.getConfig());
        }
    }

    private ContainerInfo toContainerInfo(Container container) {
        return new ContainerInfo(container.getId(), container.getNames(), container.getImage(),
                container.getStatus(), getNetworks(container), container.getLabels(), getExposedPorts(container));
    }

    private static String[] getNetworks(Container container) {
        ContainerNetworkSettings networkSettings = container.getNetworkSettings();
        if (networkSettings == null) {
            return null;
        }
        Map<String, ContainerNetwork> networks = networkSettings.getNetworks();
        if (networks == null) {
            return null;
        }
        return networks.entrySet().stream()
                .map(e -> {
                    List<String> aliases = e.getValue().getAliases();
                    if (aliases == null) {
                        return e.getKey();
                    }
                    return e.getKey() + " (" + String.join(",", aliases) + ")";
                })
                .toArray(String[]::new);
    }

    private ContainerInfo.ContainerPort[] getExposedPorts(Container container) {
        return Arrays.stream(container.getPorts())
                .map(c -> new ContainerInfo.ContainerPort(c.getIp(), c.getPrivatePort(), c.getPublicPort(), c.getType()))
                .toArray(ContainerInfo.ContainerPort[]::new);
    }

    private synchronized void toggleLogForwarders() {
        if (logForwardEnabled) {
            for (ContainerLogForwarder logForwarder : containerLogForwarders.values()) {
                if (logForwarder.isRunning()) {
                    logForwarder.close();
                }
            }
            logForwardEnabled = false;
        } else {
            for (ContainerLogForwarder logForwarder : containerLogForwarders.values()) {
                logForwarder.start();
            }
            logForwardEnabled = true;
        }
    }

    public static void printDevService(StringBuilder builder, DevServiceDescriptionBuildItem devService, boolean withStatus) {
        builder.append(BOLD).append(devService.getName()).append(NO_BOLD);
        builder.append("\n");

        if (devService.hasContainerInfo()) {
            builder.append(String.format("  %-18s", "Container: "))
                    .append(devService.getContainerInfo().getId(), 0, 12)
                    .append(devService.getContainerInfo().formatNames())
                    .append("  ")
                    .append(devService.getContainerInfo().getImageName())
                    .append("\n");
            builder.append(String.format("  %-18s", "Network: "))
                    .append(devService.getContainerInfo().formatNetworks())
                    .append(" - ")
                    .append(devService.getContainerInfo().formatPorts())
                    .append("\n");

            ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime(false);
            if (containerRuntime != null) {
                builder.append(String.format("  %-18s", "Exec command: "))
                        .append(String.format(EXEC_FORMAT,
                                containerRuntime.getExecutableName(),
                                devService.getContainerInfo().getShortId()))
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
