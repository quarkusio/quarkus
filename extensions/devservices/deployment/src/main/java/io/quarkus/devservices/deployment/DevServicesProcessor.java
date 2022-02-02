package io.quarkus.devservices.deployment;

import static io.quarkus.deployment.dev.testing.MessageFormat.BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_BOLD;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_UNDERLINE;
import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.MessageFormat.UNDERLINE;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devservices.runtime.devmode.ContainerInfo;
import io.quarkus.devservices.runtime.devmode.DevServiceDescription;
import io.quarkus.devservices.runtime.devmode.DevServicesRecorder;

public class DevServicesProcessor {

    private static final String EXEC_FORMAT = "docker exec -it %s /bin/bash";

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    static volatile ConsoleStateManager.ConsoleContext context;
    static volatile boolean logForwardEnabled = false;
    static Map<String, ContainerLogForwarder> containerLogForwarders = new HashMap<>();

    @BuildStep(onlyIf = { IsDevelopment.class })
    @Record(value = ExecutionTime.STATIC_INIT)
    public void config(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeTemplateProducer,
            BuildProducer<ConsoleCommandBuildItem> commandBuildItemBuildProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            DevServicesRecorder recorder,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            List<DevServicesResultBuildItem> devServicesResults) {
        List<DevServiceDescription> serviceDescriptions = buildServiceDescriptions(devServicesResults,
                devServicesLauncherConfig);

        for (DevServiceDescription devService : serviceDescriptions) {
            if (devService.hasContainerInfo()) {
                containerLogForwarders.compute(devService.getContainerInfo().getId(),
                        (id, forwarder) -> Objects.requireNonNullElseGet(forwarder,
                                () -> new ContainerLogForwarder(devService)));
            }
        }

        devConsoleRuntimeTemplateProducer.produce(new DevConsoleRuntimeTemplateInfoBuildItem("devServices",
                recorder.devServices(serviceDescriptions), this.getClass(), curateOutcomeBuildItem));

        // Build commands if we are in local dev mode
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }

        commandBuildItemBuildProducer.produce(
                new ConsoleCommandBuildItem(new DevServicesCommand(serviceDescriptions)));

        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("Dev Services");
        }
        context.reset(
                new ConsoleCommand('c', "Show dev services containers", null, () -> {
                    List<DevServiceDescription> descriptions = buildServiceDescriptions(devServicesResults,
                            devServicesLauncherConfig);
                    StringBuilder builder = new StringBuilder();
                    builder.append("\n\n")
                            .append(RED + "==" + RESET + " " + UNDERLINE + "Dev Services" + NO_UNDERLINE)
                            .append("\n\n");
                    for (DevServiceDescription devService : descriptions) {
                        printDevService(builder, devService, true);
                        builder.append("\n");
                    }
                    System.out.println(builder);
                }),
                new ConsoleCommand('g', "Follow dev services logs to the console",
                        new ConsoleCommand.HelpState(() -> logForwardEnabled ? GREEN : RED,
                                () -> logForwardEnabled ? "enabled" : "disabled"),
                        this::toggleLogForwarders));
    }

    private List<DevServiceDescription> buildServiceDescriptions(List<DevServicesResultBuildItem> devServicesResults,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {
        // Fetch container infos
        Set<String> containerIds = devServicesResults.stream()
                .map(DevServicesResultBuildItem::getContainerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Container> containerInfos = fetchContainerInfos(containerIds);
        // Build descriptions
        Set<String> configKeysFromDevServices = new HashSet<>();
        List<DevServiceDescription> descriptions = new ArrayList<>();
        for (DevServicesResultBuildItem buildItem : devServicesResults) {
            configKeysFromDevServices.addAll(buildItem.getConfig().keySet());
            descriptions.add(toDevServiceDescription(buildItem, containerInfos.get(buildItem.getContainerId())));
        }
        // Sort descriptions by name
        descriptions.sort(Comparator.comparing(DevServiceDescription::getName));
        // Add description from other dev service configs as last
        if (devServicesLauncherConfig.isPresent()) {
            Map<String, String> config = new HashMap<>(devServicesLauncherConfig.get().getConfig());
            for (String key : configKeysFromDevServices) {
                config.remove(key);
            }
            if (!config.isEmpty()) {
                descriptions.add(new DevServiceDescription("Other Dev Services", null, config));
            }
        }
        return descriptions;
    }

    private Map<String, Container> fetchContainerInfos(Set<String> containerIds) {
        if (containerIds.isEmpty() || !isDockerWorking.getAsBoolean()) {
            return Collections.emptyMap();
        }
        return DockerClientFactory.lazyClient().listContainersCmd()
                .withIdFilter(containerIds)
                .withShowAll(true)
                .exec()
                .stream()
                .collect(Collectors.toMap(Container::getId, Function.identity()));
    }

    private DevServiceDescription toDevServiceDescription(DevServicesResultBuildItem buildItem, Container container) {
        if (container == null) {
            return new DevServiceDescription(buildItem.getName(), null, buildItem.getConfig());
        } else {
            return new DevServiceDescription(buildItem.getName(), toContainerInfo(container), buildItem.getConfig());
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

    public static void printDevService(StringBuilder builder, DevServiceDescription devService, boolean withStatus) {
        if (devService.hasContainerInfo()) {
            builder.append(BOLD).append(devService.getName()).append(NO_BOLD);
            if (withStatus) {
                builder.append(" - ").append(devService.getContainerInfo().getStatus());
            }
            builder.append("\n");
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
            builder.append(String.format("  %-18s", "Exec command: "))
                    .append(String.format(EXEC_FORMAT, devService.getContainerInfo().getShortId()))
                    .append("\n");
        }
        builder.append(String.format("  %-18s", "Injected Config: "))
                .append(devService.formatConfigs())
                .append("\n");
    }

}
