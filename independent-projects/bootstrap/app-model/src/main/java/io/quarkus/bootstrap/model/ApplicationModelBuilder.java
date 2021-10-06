package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

public class ApplicationModelBuilder {

    public static final String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    public static final String RUNNER_PARENT_FIRST_ARTIFACTS = "runner-parent-first-artifacts";
    public static final String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    public static final String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

    private static final Logger log = Logger.getLogger(ApplicationModelBuilder.class);

    ResolvedDependency appArtifact;

    final Map<ArtifactKey, ResolvedDependency> dependencies = new LinkedHashMap<>();
    final Set<ArtifactKey> parentFirstArtifacts = new HashSet<>();
    final Set<ArtifactKey> runnerParentFirstArtifacts = new HashSet<>();
    final Set<ArtifactKey> excludedArtifacts = new HashSet<>();
    final Set<ArtifactKey> lesserPriorityArtifacts = new HashSet<>();
    final Set<ArtifactKey> reloadableWorkspaceModules = new HashSet<>();
    final List<ExtensionCapabilities> extensionCapabilities = new ArrayList<>();
    PlatformImports platformImports;
    final Map<WorkspaceModuleId, DefaultWorkspaceModule> projectModules = new HashMap<>();

    private Predicate<ResolvedDependency> depPredicate;

    public ApplicationModelBuilder setAppArtifact(ResolvedDependency appArtifact) {
        this.appArtifact = appArtifact;
        return this;
    }

    public ApplicationModelBuilder setPlatformImports(PlatformImports platformImports) {
        this.platformImports = platformImports;
        return this;
    }

    public ApplicationModelBuilder addExtensionCapabilities(ExtensionCapabilities extensionCapabilities) {
        this.extensionCapabilities.add(extensionCapabilities);
        return this;
    }

    public ApplicationModelBuilder addDependency(ResolvedDependency dep) {
        dependencies.put(dep.getKey(), dep);
        return this;
    }

    public ApplicationModelBuilder addDependencies(Collection<ResolvedDependency> deps) {
        deps.forEach(d -> addDependency(d));
        return this;
    }

    public Dependency getDependency(ArtifactKey key) {
        return dependencies.get(key);
    }

    public ApplicationModelBuilder addParentFirstArtifact(ArtifactKey deps) {
        this.parentFirstArtifacts.add(deps);
        return this;
    }

    public ApplicationModelBuilder addParentFirstArtifacts(List<ArtifactKey> deps) {
        this.parentFirstArtifacts.addAll(deps);
        return this;
    }

    public ApplicationModelBuilder addRunnerParentFirstArtifact(ArtifactKey deps) {
        this.runnerParentFirstArtifacts.add(deps);
        return this;
    }

    public ApplicationModelBuilder addRunnerParentFirstArtifacts(List<ArtifactKey> deps) {
        this.runnerParentFirstArtifacts.addAll(deps);
        return this;
    }

    public ApplicationModelBuilder addExcludedArtifact(ArtifactKey deps) {
        this.excludedArtifacts.add(deps);
        return this;
    }

    public ApplicationModelBuilder addExcludedArtifacts(List<ArtifactKey> deps) {
        this.excludedArtifacts.addAll(deps);
        return this;
    }

    public ApplicationModelBuilder addLesserPriorityArtifact(ArtifactKey deps) {
        this.lesserPriorityArtifacts.add(deps);
        return this;
    }

    public ApplicationModelBuilder addReloadableWorkspaceModule(ArtifactKey key) {
        this.reloadableWorkspaceModules.add(key);
        return this;
    }

    public ApplicationModelBuilder addReloadableWorkspaceModules(Collection<ArtifactKey> key) {
        this.reloadableWorkspaceModules.addAll(key);
        return this;
    }

    public ApplicationModelBuilder addLesserPriorityArtifacts(List<ArtifactKey> deps) {
        this.lesserPriorityArtifacts.addAll(deps);
        return this;
    }

    public DefaultWorkspaceModule getOrCreateProjectModule(WorkspaceModuleId id, File moduleDir, File buildDir) {
        return projectModules.computeIfAbsent(id, k -> new DefaultWorkspaceModule(id, moduleDir, buildDir));
    }

    /**
     * Sets the parent first and excluded artifacts from a descriptor properties file
     *
     * @param props The quarkus-extension.properties file
     */
    public void handleExtensionProperties(Properties props, String extension) {
        String parentFirst = props.getProperty(PARENT_FIRST_ARTIFACTS);
        if (parentFirst != null) {
            String[] artifacts = parentFirst.split(",");
            for (String artifact : artifacts) {
                parentFirstArtifacts.add(new GACT(artifact.split(":")));
            }
        }
        String runnerParentFirst = props.getProperty(RUNNER_PARENT_FIRST_ARTIFACTS);
        if (runnerParentFirst != null) {
            String[] artifacts = runnerParentFirst.split(",");
            for (String artifact : artifacts) {
                runnerParentFirstArtifacts.add(new GACT(artifact.split(":")));
            }
        }
        String excluded = props.getProperty(EXCLUDED_ARTIFACTS);
        if (excluded != null) {
            String[] artifacts = excluded.split(",");
            for (String artifact : artifacts) {
                excludedArtifacts.add(new GACT(artifact.split(":")));
                log.debugf("Extension %s is excluding %s", extension, artifact);
            }
        }
        String lesserPriority = props.getProperty(LESSER_PRIORITY_ARTIFACTS);
        if (lesserPriority != null) {
            String[] artifacts = lesserPriority.split(",");
            for (String artifact : artifacts) {
                lesserPriorityArtifacts.add(new GACT(artifact.split(":")));
                log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
            }
        }
    }

    private Predicate<ResolvedDependency> dependencyPredicate() {
        if (depPredicate == null) {
            depPredicate = s -> {
                // we never include the ide launcher in the final app model
                if (s.getGroupId().equals("io.quarkus")
                        && s.getArtifactId().equals("quarkus-ide-launcher")) {
                    return false;
                }
                return !excludedArtifacts.contains(s.getKey());
            };
        }
        return depPredicate;
    }

    List<ResolvedDependency> filter(Collection<ResolvedDependency> deps) {
        return deps.stream().filter(dependencyPredicate()).collect(Collectors.toList());
    }

    public DefaultApplicationModel build() {
        return new DefaultApplicationModel(this);
    }
}
