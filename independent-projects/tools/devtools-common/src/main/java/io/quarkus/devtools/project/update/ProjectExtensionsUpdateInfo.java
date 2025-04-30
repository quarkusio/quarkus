package io.quarkus.devtools.project.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;

public class ProjectExtensionsUpdateInfo {
    private final Map<String, List<ExtensionUpdateInfo>> extensionsByProvider;

    public ProjectExtensionsUpdateInfo(Map<String, List<ExtensionUpdateInfo>> extensionsByProvider) {
        this.extensionsByProvider = extensionsByProvider;
    }

    public Map<String, List<ExtensionUpdateInfo>> extensionsByProvider() {
        return extensionsByProvider;
    }

    public boolean containsProvider(String provider) {
        return extensionsByProvider.containsKey(provider);
    }

    public OptionalInt getMinJavaVersion() {
        return streamExtensions()
                .mapToInt(e -> Optional.ofNullable(ExtensionProcessor.getMinimumJavaVersion(e.getRecommendedMetadata()))
                        .orElse(JavaVersion.DEFAULT_JAVA_VERSION))
                .max();
    }

    private Stream<ExtensionUpdateInfo> streamExtensions() {
        return extensionsByProvider.values().stream()
                .flatMap(Collection::stream);
    }

    public List<ExtensionUpdateInfo> getSimpleVersionUpdates() {
        return streamExtensions().filter(ExtensionUpdateInfo::isSimpleVersionUpdate)
                .filter(ExtensionUpdateInfo::isUpdateRecommended).collect(Collectors.toList());
    }

    public List<ExtensionUpdateInfo> getVersionUpdates() {
        return streamExtensions().filter(ExtensionUpdateInfo::isVersionUpdate).collect(Collectors.toList());
    }

    public boolean shouldUpdateExtensions() {
        return streamExtensions().anyMatch(ExtensionUpdateInfo::shouldUpdateExtension);
    }
}
