package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.io.Serializable;
import java.util.Set;

public class ConfiguredClassLoading implements Serializable {

    public final Set<AppArtifactKey> parentFirstArtifacts;
    public final Set<AppArtifactKey> reloadableArtifacts;
    public final boolean flatTestClassPath;

    public ConfiguredClassLoading(Set<AppArtifactKey> parentFirstArtifacts, Set<AppArtifactKey> reloadableArtifacts,
            boolean flatTestClassPath) {
        this.parentFirstArtifacts = parentFirstArtifacts;
        this.reloadableArtifacts = reloadableArtifacts;
        this.flatTestClassPath = flatTestClassPath;
    }
}
