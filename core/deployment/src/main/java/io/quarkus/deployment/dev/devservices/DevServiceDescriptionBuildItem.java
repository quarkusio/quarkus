package io.quarkus.deployment.dev.devservices;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServiceDescriptionBuildItem extends MultiBuildItem {
    private String name;
    private String description;
    private Supplier<ContainerInfo> lazyContainerInfo;
    private Map<String, String> configs;
    private final Supplier<Map<String, String>> lazyConfigs;

    public DevServiceDescriptionBuildItem() {
        this.lazyConfigs = null;
    }

    public DevServiceDescriptionBuildItem(String name, String description, ContainerInfo containerInfo,
            Map<String, String> configs) {
        this(name, description, () -> containerInfo, configs, null);
    }

    public DevServiceDescriptionBuildItem(String name, Supplier<ContainerInfo> lazyContainerInfo,
            Map<String, String> configs, Supplier<Map<String, String>> lazyConfigs) {
        this(name, null, lazyContainerInfo, configs, lazyConfigs);
    }

    public DevServiceDescriptionBuildItem(String name, String description, Supplier<ContainerInfo> lazyContainerInfo,
            Map<String, String> configs, Supplier<Map<String, String>> lazyConfigs) {
        this.name = name;
        this.description = description;
        this.lazyContainerInfo = lazyContainerInfo;
        this.configs = configs instanceof SortedMap ? configs : new TreeMap<>(configs);
        this.lazyConfigs = lazyConfigs;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ContainerInfo getContainerInfo() {
        return lazyContainerInfo != null ? lazyContainerInfo.get() : null;
    }

    public Map<String, String> getConfigs() {
        if (lazyConfigs == null) {
            return configs;
        } else {
            Map<String, String> result = new TreeMap<>(configs);
            // This will make sure the lazy config is sorted even though we couldn't sort it up-front
            result.putAll(lazyConfigs.get());
            return result;
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
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
