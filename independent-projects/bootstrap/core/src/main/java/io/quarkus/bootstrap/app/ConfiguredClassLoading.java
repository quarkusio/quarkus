package io.quarkus.bootstrap.app;

import io.quarkus.maven.dependency.ArtifactKey;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfiguredClassLoading implements Serializable {

    public final Set<ArtifactKey> parentFirstArtifacts;
    public final Set<ArtifactKey> reloadableArtifacts;
    public final Set<ArtifactKey> removedArtifacts;
    public final Map<ArtifactKey, List<String>> removedResources;
    public final boolean flatTestClassPath;

    public ConfiguredClassLoading(Set<ArtifactKey> parentFirstArtifacts, Set<ArtifactKey> reloadableArtifacts,
            Set<ArtifactKey> removedArtifacts,
            Map<ArtifactKey, List<String>> removedResources, boolean flatTestClassPath) {
        this.parentFirstArtifacts = parentFirstArtifacts;
        this.reloadableArtifacts = reloadableArtifacts;
        this.removedResources = removedResources;
        this.flatTestClassPath = flatTestClassPath;
        this.removedArtifacts = removedArtifacts;
    }
}
