package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfiguredClassLoading implements Serializable {

    public final Set<AppArtifactKey> parentFirstArtifacts;
    public final Set<AppArtifactKey> reloadableArtifacts;
    public final Map<AppArtifactKey, List<String>> removedResources;
    public final boolean flatTestClassPath;

    public ConfiguredClassLoading(Set<AppArtifactKey> parentFirstArtifacts, Set<AppArtifactKey> reloadableArtifacts,
            Map<AppArtifactKey, List<String>> removedResources, boolean flatTestClassPath) {
        this.parentFirstArtifacts = parentFirstArtifacts;
        this.reloadableArtifacts = reloadableArtifacts;
        this.removedResources = removedResources;
        this.flatTestClassPath = flatTestClassPath;
    }
}
