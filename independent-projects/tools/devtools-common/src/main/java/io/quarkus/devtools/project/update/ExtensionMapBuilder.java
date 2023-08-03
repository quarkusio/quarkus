package io.quarkus.devtools.project.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.Extension;

final class ExtensionMapBuilder {
    final Map<String, List<ExtensionUpdateInfoBuilder>> extensionInfo;
    final List<ExtensionUpdateInfoBuilder> list = new ArrayList<>();

    public ExtensionMapBuilder() {
        this.extensionInfo = new LinkedHashMap<>();
    }

    public ExtensionMapBuilder(int size) {
        this.extensionInfo = new LinkedHashMap<>(size);
    }

    public void add(ExtensionUpdateInfoBuilder e) {
        extensionInfo.put(e.currentDep.getArtifact().getArtifactId(), Collections.singletonList(e));
        list.add(e);
    }

    public ExtensionUpdateInfoBuilder get(ArtifactKey key) {
        final List<ExtensionUpdateInfoBuilder> list = extensionInfo.get(key.getArtifactId());
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        for (ExtensionUpdateInfoBuilder e : list) {
            final TopExtensionDependency recommendedDep = e.resolveRecommendedDep();
            if (e.currentDep.getKey().equals(key)
                    || recommendedDep != null && recommendedDep.getKey().equals(key)) {
                return e;
            }
        }
        throw new IllegalArgumentException(key + " isn't found in the extension map");
    }

    public Collection<ExtensionUpdateInfoBuilder> values() {
        return list;
    }

    public int size() {
        return extensionInfo.size();
    }

    public boolean isEmpty() {
        return extensionInfo.isEmpty();
    }

    public static final class ExtensionUpdateInfoBuilder {
        private final TopExtensionDependency currentDep;
        private Extension recommendedMetadata;
        private TopExtensionDependency recommendedDep;

        private Extension latestMetadata;

        public ExtensionUpdateInfoBuilder(TopExtensionDependency currentDep) {
            this.currentDep = currentDep;
        }

        public TopExtensionDependency getCurrentDep() {
            return currentDep;
        }

        public Extension getRecommendedMetadata() {
            return recommendedMetadata;
        }

        public void setRecommendedMetadata(Extension e) {
            this.recommendedMetadata = e;
        }

        public ExtensionUpdateInfoBuilder setRecommendedDep(TopExtensionDependency recommendedDep) {
            this.recommendedDep = recommendedDep;
            return this;
        }

        public ExtensionUpdateInfo build() {
            final TopExtensionDependency effectiveRecommendedDep = resolveRecommendedDep();
            return new ExtensionUpdateInfo(currentDep, effectiveRecommendedDep.getCatalogMetadata(), effectiveRecommendedDep);
        }

        public TopExtensionDependency resolveRecommendedDep() {
            if (recommendedDep != null) {
                return recommendedDep;
            }
            return recommendedMetadata == null ? currentDep
                    : TopExtensionDependency.builder()
                            .setArtifact(recommendedMetadata.getArtifact())
                            .setCatalogMetadata(recommendedMetadata)
                            .setTransitive(currentDep.isTransitive())
                            .build();
        }

        public Extension getLatestMetadata() {
            return latestMetadata;
        }

        public ExtensionUpdateInfoBuilder setLatestMetadata(Extension latestMetadata) {
            this.latestMetadata = latestMetadata;
            return this;
        }
    }

}
