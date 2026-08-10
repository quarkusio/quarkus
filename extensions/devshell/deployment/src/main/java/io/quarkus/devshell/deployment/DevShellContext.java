package io.quarkus.devshell.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.devshell.deployment.tui.screens.DependenciesScreen;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;

/**
 * Holds build-time data collected by DevShellProcessor,
 * accessible to TUI screens.
 */
public final class DevShellContext {

    private static volatile List<ExtensionsListScreen.ExtensionInfo> extensionInfos = List.of();
    private static volatile List<DependenciesScreen.DependencyInfo> dependencyInfos = List.of();
    private static volatile Map<String, ShellPageInfo> shellPages = Map.of();
    private static volatile Map<String, Map<String, Object>> buildTimeData = Map.of();

    private DevShellContext() {
    }

    public static List<ExtensionsListScreen.ExtensionInfo> getExtensionInfos() {
        return extensionInfos;
    }

    static void setExtensionInfos(List<ExtensionsListScreen.ExtensionInfo> infos) {
        extensionInfos = infos;
    }

    public static List<DependenciesScreen.DependencyInfo> getDependencyInfos() {
        return dependencyInfos;
    }

    static void setDependencyInfos(List<DependenciesScreen.DependencyInfo> infos) {
        dependencyInfos = infos;
    }

    public static Map<String, ShellPageInfo> getShellPages() {
        return shellPages;
    }

    static void setShellPages(Map<String, ShellPageInfo> pages) {
        shellPages = pages;
    }

    public static Map<String, Map<String, Object>> getBuildTimeData() {
        return buildTimeData;
    }

    public static Object getBuildTimeData(String namespace, String key) {
        Map<String, Object> nsData = buildTimeData.get(namespace);
        return nsData != null ? nsData.get(key) : null;
    }

    static void setBuildTimeData(Map<String, Map<String, Object>> data) {
        buildTimeData = data;
    }

    public record ShellPageInfo(String id, String title, String customPageClassName, Class<?> customPageClass,
            String providerClassName) {
    }
}
