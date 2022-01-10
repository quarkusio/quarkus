package io.quarkus.devservices.runtime.devmode;

import java.util.Map;
import java.util.stream.Collectors;

public class DevServiceDescription {
    private String name;
    private ContainerInfo containerInfo;
    private Map<String, String> configs;

    public DevServiceDescription() {
    }

    public DevServiceDescription(String name, ContainerInfo containerInfo, Map<String, String> configs) {
        this.name = name;
        this.containerInfo = containerInfo;
        this.configs = configs;
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
