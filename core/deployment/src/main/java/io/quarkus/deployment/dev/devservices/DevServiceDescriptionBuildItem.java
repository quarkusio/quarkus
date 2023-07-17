package io.quarkus.deployment.dev.devservices;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServiceDescriptionBuildItem extends MultiBuildItem {
    private String name;
    private ContainerInfo containerInfo;
    private Map<String, String> configs;

    public DevServiceDescriptionBuildItem() {
    }

    public DevServiceDescriptionBuildItem(String name, ContainerInfo containerInfo, Map<String, String> configs) {
        this.name = name;
        this.containerInfo = containerInfo;
        this.configs = configs instanceof SortedMap ? configs : new TreeMap<>(configs);
    }

    public boolean hasContainerInfo() {
        return containerInfo != null;
    }

    public String getName() {
        return name;
    }

    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContainerInfo(ContainerInfo containerInfo) {
        this.containerInfo = containerInfo;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    public String formatConfigs() {
        return configs.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
