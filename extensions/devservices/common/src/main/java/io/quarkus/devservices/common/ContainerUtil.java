package io.quarkus.devservices.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;

import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.RunningContainer;

/**
 * Utility class for working with containers.
 */
public class ContainerUtil {

    private static final int CONTAINER_SHORT_ID_LENGTH = 12;

    /**
     * Convert an InspectContainerResponse to a RunningContainer.
     *
     * @param inspectContainer The container inspect response.
     * @return The running container.
     */
    public static RunningContainer toRunningContainer(InspectContainerResponse inspectContainer) {
        return new RunningContainer(toContainerInfo(inspectContainer), getContainerEnv(inspectContainer));
    }

    /**
     * Convert an InspectContainerResponse to a ContainerInfo.
     *
     * @param inspectContainer The container inspect response.
     * @return The container info.
     */
    public static ContainerInfo toContainerInfo(InspectContainerResponse inspectContainer) {
        String[] names = inspectContainer.getNetworkSettings().getNetworks().values().stream()
                .flatMap(c -> c.getAliases() == null ? Stream.of() : c.getAliases().stream())
                .toArray(String[]::new);
        return new ContainerInfo(inspectContainer.getId(), names, inspectContainer.getConfig().getImage(),
                inspectContainer.getState().getStatus(), getNetworks(inspectContainer),
                inspectContainer.getConfig().getLabels(),
                getExposedPorts(inspectContainer));
    }

    private static Map<String, String[]> getNetworks(InspectContainerResponse container) {
        NetworkSettings networkSettings = container.getNetworkSettings();
        if (networkSettings == null) {
            return null;
        }
        return getNetworks(networkSettings.getNetworks());
    }

    public static Map<String, String[]> getNetworks(Map<String, ContainerNetwork> networkSettings) {
        if (networkSettings == null) {
            return null;
        }
        return networkSettings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> {
                            List<String> aliases = e.getValue().getAliases();
                            return aliases == null ? new String[0] : aliases.toArray(new String[0]);
                        }));
    }

    private static ContainerInfo.ContainerPort[] getExposedPorts(InspectContainerResponse inspectContainer) {
        return inspectContainer.getNetworkSettings().getPorts().getBindings().entrySet().stream()
                .filter(e -> e.getValue() != null)
                .flatMap(e -> Arrays.stream(e.getValue())
                        .map(b -> new ContainerInfo.ContainerPort(b.getHostIp(),
                                e.getKey().getPort(),
                                Integer.parseInt(b.getHostPortSpec()),
                                e.getKey().getProtocol().toString())))
                .toArray(ContainerInfo.ContainerPort[]::new);
    }

    /**
     * Get the environment variables for a container.
     *
     * @param inspectContainer The container info.
     * @return A map of environment variables to their values.
     */
    public static Map<String, String> getContainerEnv(InspectContainerResponse inspectContainer) {
        String[] env = inspectContainer.getConfig().getEnv();
        if (env == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(env)
                .map(e -> e.split("=", 2))
                .collect(Collectors.toMap(e -> e[0], e -> e.length > 1 ? e[1] : ""));
    }

    /**
     * Get the environment variable configuration for a list of containers.
     *
     * @param instances A list of suppliers that provide the container info.
     * @param envVarMappingHint A function that maps the container environment variable to config key.
     * @return A map of config keys to their values.
     */
    public static Map<String, String> getEnvVarConfig(List<? extends Supplier<InspectContainerResponse>> instances,
            Function<InspectContainerResponse, Map<String, String>> envVarMappingHint) {
        Map<String, String> configs = new HashMap<>();
        for (Supplier<InspectContainerResponse> containerResponseSupplier : instances) {
            configs.putAll(getEnvVarConfig(containerResponseSupplier, envVarMappingHint));
        }
        return configs;
    }

    /**
     * Get the environment variable configuration for a container.
     *
     * @param containerInfoSupplier A supplier that provides the container info.
     * @param envVarMappingHint A function that maps the container environment variable to config key.
     * @return A map of environment variables to their values.
     */
    public static Map<String, String> getEnvVarConfig(Supplier<InspectContainerResponse> containerInfoSupplier,
            Function<InspectContainerResponse, Map<String, String>> envVarMappingHint) {
        Map<String, String> configs = new HashMap<>();
        InspectContainerResponse containerInfo = containerInfoSupplier.get();
        // container env var -> env var
        Map<String, String> mappings = envVarMappingHint.apply(containerInfo);
        if (mappings != null && !mappings.isEmpty()) {
            Map<String, String> containerEnv = getContainerEnv(containerInfo);
            mappings.forEach((k, v) -> {
                String value = containerEnv.get(k);
                if (value != null) {
                    configs.put(v, value);
                }
            });
        }
        return configs;
    }

    /**
     * Get the port configuration for a list of containers.
     *
     * @param instances A list of suppliers that provide the container info.
     * @param envVarMappingHint A function that maps the container port to a config key.
     * @return A map of config keys to their values.
     */
    public static Map<String, String> getPortConfig(List<? extends Supplier<InspectContainerResponse>> instances,
            Function<InspectContainerResponse, Map<Integer, String>> envVarMappingHint) {
        Map<String, String> configs = new HashMap<>();
        for (Supplier<InspectContainerResponse> containerResponseSupplier : instances) {
            configs.putAll(getPortConfig(containerResponseSupplier, envVarMappingHint));
        }
        return configs;
    }

    /**
     * Get the port configuration for a container.
     *
     * @param containerResponseSupplier A supplier that provides the container info.
     * @param envVarMappingHint A function that maps the container port to a config key.
     * @return A map of config keys to their values.
     */
    public static Map<String, String> getPortConfig(Supplier<InspectContainerResponse> containerResponseSupplier,
            Function<InspectContainerResponse, Map<Integer, String>> envVarMappingHint) {
        Map<String, String> configs = new HashMap<>();
        InspectContainerResponse containerInfo = containerResponseSupplier.get();
        // container port -> env var
        Map<Integer, String> mappings = envVarMappingHint.apply(containerInfo);
        if (mappings != null && !mappings.isEmpty()) {
            mappings.forEach((containerPort, envVar) -> {
                for (ContainerInfo.ContainerPort exposedPort : getExposedPorts(containerInfo)) {
                    if (Objects.equals(exposedPort.privatePort(), containerPort)) {
                        configs.put(envVar, String.valueOf(exposedPort.publicPort()));
                        break;
                    }
                }
            });
        }
        return configs;
    }

    public static String getShortId(String id) {
        return id.length() > CONTAINER_SHORT_ID_LENGTH ? id.substring(0, CONTAINER_SHORT_ID_LENGTH) : id;
    }
}
