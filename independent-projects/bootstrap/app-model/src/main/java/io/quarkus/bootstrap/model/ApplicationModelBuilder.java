package io.quarkus.bootstrap.model;

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

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactCoordsPattern;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class ApplicationModelBuilder {

    public static final String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    public static final String RUNNER_PARENT_FIRST_ARTIFACTS = "runner-parent-first-artifacts";
    public static final String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    public static final String REMOVED_RESOURCES_DOT = "removed-resources.";
    public static final String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

    private static final Logger log = Logger.getLogger(ApplicationModelBuilder.class);

    ResolvedDependency appArtifact;

    final Map<ArtifactKey, ResolvedDependencyBuilder> dependencies = new LinkedHashMap<>();
    final Set<ArtifactKey> parentFirstArtifacts = new HashSet<>();
    final Set<ArtifactKey> runnerParentFirstArtifacts = new HashSet<>();
    final List<ArtifactCoordsPattern> excludedArtifacts = new ArrayList<>();
    final Map<ArtifactKey, Set<String>> excludedResources = new HashMap<>(0);
    final Set<ArtifactKey> lesserPriorityArtifacts = new HashSet<>();
    final Set<ArtifactKey> reloadableWorkspaceModules = new HashSet<>();
    final List<ExtensionCapabilities> extensionCapabilities = new ArrayList<>();
    PlatformImports platformImports;
    final Map<WorkspaceModuleId, WorkspaceModule.Mutable> projectModules = new HashMap<>();

    public ApplicationModelBuilder() {
        // we never include the ide launcher in the final app model
        excludedArtifacts.add(ArtifactCoordsPattern.builder()
                .setGroupId("io.quarkus")
                .setArtifactId("quarkus-ide-launcher")
                .build());
    }

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

    public ApplicationModelBuilder addDependency(ResolvedDependencyBuilder dep) {
        dependencies.put(dep.getKey(), dep);
        return this;
    }

    public ApplicationModelBuilder addDependencies(Collection<ResolvedDependencyBuilder> deps) {
        deps.forEach(d -> addDependency(d));
        return this;
    }

    public ResolvedDependencyBuilder getDependency(ArtifactKey key) {
        return dependencies.get(key);
    }

    public Collection<ResolvedDependencyBuilder> getDependencies() {
        return dependencies.values();
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

    public ApplicationModelBuilder addExcludedArtifact(ArtifactKey key) {
        this.excludedArtifacts.add(ArtifactCoordsPattern.builder()
                .setGroupId(key.getGroupId())
                .setArtifactId(key.getArtifactId())
                .setClassifier(key.getClassifier())
                .setType(key.getType())
                .build());
        return this;
    }

    public ApplicationModelBuilder addExcludedArtifacts(List<ArtifactKey> keys) {
        for (var key : keys) {
            addExcludedArtifact(key);
        }
        return this;
    }

    public ApplicationModelBuilder addRemovedResources(ArtifactKey key, Set<String> resources) {
        this.excludedResources.computeIfAbsent(key, k -> new HashSet<>(resources.size())).addAll(resources);
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

    public WorkspaceModule.Mutable getOrCreateProjectModule(WorkspaceModuleId id, File moduleDir, File buildDir) {
        return projectModules.computeIfAbsent(id,
                k -> WorkspaceModule.builder().setModuleId(id).setModuleDir(moduleDir.toPath())
                        .setBuildDir(buildDir.toPath()));
    }

    /**
     * Sets the parent first and excluded artifacts from a descriptor properties file
     *
     * @param props The quarkus-extension.properties file
     */
    public void handleExtensionProperties(Properties props, String extension) {
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            if (prop.getValue() == null) {
                continue;
            }
            final String value = prop.getValue().toString();
            if (value.isBlank()) {
                continue;
            }
            final String name = prop.getKey().toString();
            switch (name) {
                case PARENT_FIRST_ARTIFACTS:
                    for (String artifact : value.split(",")) {
                        parentFirstArtifacts.add(new GACT(artifact.split(":")));
                    }
                    break;
                case RUNNER_PARENT_FIRST_ARTIFACTS:
                    for (String artifact : value.split(",")) {
                        runnerParentFirstArtifacts.add(new GACT(artifact.split(":")));
                    }
                    break;
                case EXCLUDED_ARTIFACTS:
                    for (String artifact : value.split(",")) {
                        excludedArtifacts.add(ArtifactCoordsPattern.of(artifact));
                        log.debugf("Extension %s is excluding %s", extension, artifact);
                    }
                    break;
                case LESSER_PRIORITY_ARTIFACTS:
                    String[] artifacts = value.split(",");
                    for (String artifact : artifacts) {
                        lesserPriorityArtifacts.add(new GACT(artifact.split(":")));
                        log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
                    }
                    break;
                default:
                    if (name.startsWith(REMOVED_RESOURCES_DOT)) {
                        final String keyStr = name.substring(REMOVED_RESOURCES_DOT.length());
                        if (!keyStr.isBlank()) {
                            ArtifactKey key = null;
                            try {
                                key = ArtifactKey.fromString(keyStr);
                            } catch (IllegalArgumentException e) {
                                log.warnf("Failed to parse artifact key %s in %s from descriptor of extension %s", keyStr, name,
                                        extension);
                            }
                            if (key != null) {
                                final Set<String> resources;
                                Collection<String> existingResources = excludedResources.get(key);
                                if (existingResources == null || existingResources.isEmpty()) {
                                    resources = Set.of(value.split(","));
                                } else {
                                    final String[] split = value.split(",");
                                    resources = new HashSet<>(existingResources.size() + split.length);
                                    resources.addAll(existingResources);
                                    for (String s : split) {
                                        resources.add(s);
                                    }
                                }
                                log.debugf("Extension %s is excluding resources %s from artifact %s", extension, resources,
                                        key);
                                excludedResources.put(key, resources);
                            }
                        }
                    }
            }
        }
    }

    private boolean isExcluded(ArtifactCoords coords) {
        for (var pattern : excludedArtifacts) {
            if (pattern.matches(coords)) {
                return true;
            }
        }
        return false;
    }

    List<ResolvedDependency> buildDependencies() {
        for (ArtifactKey key : parentFirstArtifacts) {
            final ResolvedDependencyBuilder d = dependencies.get(key);
            if (d != null) {
                d.setFlags(DependencyFlags.CLASSLOADER_PARENT_FIRST);
            }
        }
        for (ArtifactKey key : runnerParentFirstArtifacts) {
            final ResolvedDependencyBuilder d = dependencies.get(key);
            if (d != null) {
                d.setFlags(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST);
            }
        }
        for (ArtifactKey key : lesserPriorityArtifacts) {
            final ResolvedDependencyBuilder d = dependencies.get(key);
            if (d != null) {
                d.setFlags(DependencyFlags.CLASSLOADER_LESSER_PRIORITY);
            }
        }

        final List<ResolvedDependency> result = new ArrayList<>(dependencies.size());
        for (ResolvedDependencyBuilder db : this.dependencies.values()) {
            if (!isExcluded(db.getArtifactCoords())) {
                result.add(db.build());
            }
        }
        return result;
    }

    public DefaultApplicationModel build() {
        return new DefaultApplicationModel(this);
    }
}
