package io.quarkus.bootstrap.workspace;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public class DefaultArtifactSources implements ArtifactSources, Serializable {

    private static final long serialVersionUID = 2053702489268820757L;

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
