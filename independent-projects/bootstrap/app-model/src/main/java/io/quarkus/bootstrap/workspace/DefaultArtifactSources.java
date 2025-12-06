package io.quarkus.bootstrap.workspace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.BootstrapConstants;

public class DefaultArtifactSources implements ArtifactSources, Serializable {

    private static final long serialVersionUID = 2053702489268820757L;

    static ArtifactSources fromMap(Map<String, Object> map) {
        final String classifier = map.get(BootstrapConstants.MAPPABLE_CLASSIFIER).toString();

        final Collection<Map<String, Object>> sourcesMap = (Collection<Map<String, Object>>) map
                .get(BootstrapConstants.MAPPABLE_SOURCES);
        final Collection<SourceDir> sources;
        if (sourcesMap != null) {
            sources = new ArrayList<>(sourcesMap.size());
            for (Map<String, Object> sourceMap : sourcesMap) {
                sources.add(LazySourceDir.fromMap(sourceMap));
            }
        } else {
            sources = Collections.emptyList();
        }

        final Collection<Map<String, Object>> resourcesMap = (Collection<Map<String, Object>>) map
                .get(BootstrapConstants.MAPPABLE_RESOURCES);
        final Collection<SourceDir> resources;
        if (resourcesMap != null) {
            resources = new ArrayList<>(resourcesMap.size());
            for (Map<String, Object> resourceMap : resourcesMap) {
                resources.add(LazySourceDir.fromMap(resourceMap));
            }
        } else {
            resources = Collections.emptyList();
        }
        return new DefaultArtifactSources(classifier, sources, resources);
    }

    private final String classifier;
    private final Collection<SourceDir> sources;
    private final Collection<SourceDir> resources;

    public DefaultArtifactSources(String classifier, Collection<SourceDir> sources, Collection<SourceDir> resources) {
        this.classifier = Objects.requireNonNull(classifier, "The classifier is null");
        this.sources = sources;
        this.resources = resources;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public void addSources(SourceDir src) {
        this.sources.add(src);
    }

    @Override
    public Collection<SourceDir> getSourceDirs() {
        return sources;
    }

    public void addResources(SourceDir src) {
        this.resources.add(src);
    }

    @Override
    public Collection<SourceDir> getResourceDirs() {
        return resources;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(classifier);
        if (s.length() > 0) {
            s.append(' ');
        }
        s.append("sources: ").append(sources);
        s.append(" resources: ").append(resources);
        return s.toString();
    }
}
