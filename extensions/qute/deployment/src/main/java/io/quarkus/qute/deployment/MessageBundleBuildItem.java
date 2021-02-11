package io.quarkus.qute.deployment;

import java.nio.file.Path;
import java.util.Map;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBundleBuildItem extends MultiBuildItem {

    private final String name;
    private final ClassInfo defaultBundleInterface;
    private final Map<String, ClassInfo> localizedInterfaces;
    private final Map<String, Path> localizedFiles;

    public MessageBundleBuildItem(String name, ClassInfo defaultBundleInterface, Map<String, ClassInfo> localizedInterfaces,
            Map<String, Path> localizedFiles) {
        this.name = name;
        this.defaultBundleInterface = defaultBundleInterface;
        this.localizedInterfaces = localizedInterfaces;
        this.localizedFiles = localizedFiles;
    }

    public String getName() {
        return name;
    }

    public ClassInfo getDefaultBundleInterface() {
        return defaultBundleInterface;
    }

    public Map<String, ClassInfo> getLocalizedInterfaces() {
        return localizedInterfaces;
    }

    public Map<String, Path> getLocalizedFiles() {
        return localizedFiles;
    }

}
