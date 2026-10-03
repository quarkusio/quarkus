package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.RemovedResourcesBuildItem;
import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;

public class RemovedResourcesProcessor {

    private static final Logger log = Logger.getLogger(RemovedResourcesProcessor.class);

    @BuildStep
    RemovedResourcesBuildItem aggregateRemovedResources(ClassLoadingConfig classLoadingConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<RemovedResourceBuildItem> removedResourceBuildItems) {
        Map<ArtifactKey, Set<String>> removed = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classLoadingConfig.removedResources().entrySet()) {
            removed.put(new GACT(entry.getKey().split(":")), entry.getValue());
        }
        for (RemovedResourceBuildItem i : removedResourceBuildItems) {
            removed.computeIfAbsent(i.getArtifact(), k -> new HashSet<>()).addAll(i.getResources());
        }
        if (!removed.isEmpty()) {
            Map<ArtifactKey, Set<String>> validated = new HashMap<>();
            ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
            Collection<ResolvedDependency> runtimeDependencies = applicationModel.getRuntimeDependencies();
            List<ResolvedDependency> allArtifacts = new ArrayList<>(runtimeDependencies.size() + 1);
            allArtifacts.addAll(runtimeDependencies);
            allArtifacts.add(applicationModel.getAppArtifact());
            for (ResolvedDependency dep : allArtifacts) {
                Set<String> filtered = removed.remove(dep.getKey());
                if (filtered != null) {
                    validated.put(dep.getKey(), filtered);
                }
            }
            if (!removed.isEmpty()) {
                log.warn(
                        "Could not remove configured resources from the following artifacts as they were not found in the model: "
                                + removed);
            }
            return new RemovedResourcesBuildItem(validated);
        }
        return new RemovedResourcesBuildItem(Map.of());
    }
}
