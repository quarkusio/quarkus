package io.quarkus.deployment.dev.devservices;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServiceDescriptionBuildItem extends MultiBuildItem {
    private final String name;
    private final String description;
    private final Supplier<ContainerInfo> lazyContainerInfo;
    private final Supplier<Map<String, String>> lazyConfigs;

    public DevServiceDescriptionBuildItem(String name, Map<String, String> configs) {
        this(name, null, (Supplier<ContainerInfo>) null, configs);
    }

    public DevServiceDescriptionBuildItem(String name, String description, ContainerInfo containerInfo,
            Map<String, String> configs) {
        this(name, description, () -> containerInfo, configs);
    }

    public DevServiceDescriptionBuildItem(String name, Supplier<ContainerInfo> lazyContainerInfo,
            Supplier<Map<String, String>> lazyConfigs) {
        this(name, null, lazyContainerInfo, lazyConfigs);
    }

    public DevServiceDescriptionBuildItem(String name, String description, Map<String, String> config) {
        this(name, description, (Supplier<ContainerInfo>) null, config);
    }

    public DevServiceDescriptionBuildItem(String name, String description, Supplier<ContainerInfo> lazyContainerInfo,
            Map<String, String> configs) {
        this(name, description, lazyContainerInfo, () -> configs instanceof SortedMap ? configs : new TreeMap<>(configs));
    }

    public DevServiceDescriptionBuildItem(String name, String description, Supplier<ContainerInfo> lazyContainerInfo,
            Supplier<Map<String, String>> lazyConfigs) {
        this.name = name;
        this.description = description;
        this.lazyContainerInfo = lazyContainerInfo;
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
        Map<String, String> map = lazyConfigs.get();
        if (map == null) {
            return null;
        }
        return new TreeMap<>(map);
    }

}
