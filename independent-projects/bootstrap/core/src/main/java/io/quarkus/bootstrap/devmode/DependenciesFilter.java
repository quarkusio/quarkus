package io.quarkus.bootstrap.devmode;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

public class DependenciesFilter {

    private static final Logger log = Logger.getLogger(DependenciesFilter.class);

    public static List<ResolvedDependency> getReloadableModules(ApplicationModel appModel) {
        final Map<ArtifactKey, WorkspaceDependencies> modules = new HashMap<>();
        appModel.getDependencies().forEach(d -> {
            if (d.isReloadable()) {
                modules.put(d.getKey(), new WorkspaceDependencies(d));
            } else if (d.isWorkspaceModule()) {
                //if this project also contains Quarkus extensions we do no want to include these in the discovery
                //a bit of an edge case, but if you try and include a sample project with your extension you will
                //run into problems without this
                final StringBuilder msg = new StringBuilder();
                msg.append("Local Quarkus extension dependency ").append(d.toCompactCoords())
                        .append(" will not be hot-reloadable");
                log.warn(msg.toString());
            }
        });
        if (modules.isEmpty()) {
            return appModel.getApplicationModule() == null ? List.of() : List.of(appModel.getAppArtifact());
        }

        if (appModel.getApplicationModule() != null) {
            modules.put(appModel.getAppArtifact().getKey(), new WorkspaceDependencies(appModel.getAppArtifact()));
        }

        // Here we are sorting the reloadable dependencies according to their interdependencies to make sure
        // they are compiled in the correct order

        for (WorkspaceDependencies ad : modules.values()) {
            for (Dependency dd : ad.artifact.getWorkspaceModule().getDirectDependencies()) {
                final WorkspaceDependencies dep = modules.get(dd.getKey());
                if (dep != null) {
                    ad.addDependency(dep);
                }
            }
        }

        final List<ResolvedDependency> sorted = new ArrayList<>();
        int toBeSorted;
        do {
            toBeSorted = 0;
            for (WorkspaceDependencies ad : modules.values()) {
                if (ad.sorted) {
                    continue;
                }
                if (ad.hasNotSortedDependencies()) {
                    ++toBeSorted;
                    continue;
                }
                ad.sorted = true;
                sorted.add(ad.artifact);
            }
        } while (toBeSorted > 0);

        return sorted;
    }

    private static class WorkspaceDependencies {
        final ResolvedDependency artifact;
        List<WorkspaceDependencies> deps;
        boolean sorted;

        WorkspaceDependencies(ResolvedDependency artifact) {
            this.artifact = artifact;
        }

        boolean hasNotSortedDependencies() {
            if (deps == null) {
                return false;
            }
            for (WorkspaceDependencies d : deps) {
                if (!d.sorted) {
                    return true;
                }
            }
            return false;
        }

        void addDependency(WorkspaceDependencies dep) {
            if (deps == null) {
                deps = new ArrayList<>();
            }
            deps.add(dep);
        }
    }
}
