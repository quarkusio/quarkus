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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
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

    private static final String COMMA = ",";

    ResolvedDependencyBuilder appArtifact;

    final Map<ArtifactKey, ResolvedDependencyBuilder> dependencies = new LinkedHashMap<>();
    final Collection<ArtifactKey> parentFirstArtifacts = new ConcurrentLinkedDeque<>();
    final Collection<ArtifactKey> runnerParentFirstArtifacts = new ConcurrentLinkedDeque<>();
    final Collection<ArtifactCoordsPattern> excludedArtifacts = new ConcurrentLinkedDeque<>();
    final Map<ArtifactKey, Set<String>> excludedResources = new ConcurrentHashMap<>();
    final Collection<ArtifactKey> lesserPriorityArtifacts = new ConcurrentLinkedDeque<>();
    final Collection<ArtifactKey> reloadableWorkspaceModules = new ConcurrentLinkedDeque<>();
    final Collection<ExtensionCapabilities> extensionCapabilities = new ConcurrentLinkedDeque<>();
    PlatformImports platformImports;
    final Map<WorkspaceModuleId, WorkspaceModule.Mutable> projectModules = new HashMap<>();
    final Collection<ExtensionDevModeConfig> extensionDevConfig = new ConcurrentLinkedDeque<>();

    public ApplicationModelBuilder() {
        // we never include the ide launcher in the final app model
        excludedArtifacts.add(ArtifactCoordsPattern.builder()
                .setGroupId("io.quarkus")
                .setArtifactId("quarkus-ide-launcher")
                .build());
    }

    public ApplicationModelBuilder setAppArtifact(ResolvedDependencyBuilder appArtifact) {
        this.appArtifact = appArtifact;
        return this;
    }

    public ResolvedDependencyBuilder getApplicationArtifact() {
        return appArtifact;
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
        deps.forEach(this::addDependency);
        return this;
    }

    public boolean hasDependency(ArtifactKey key) {
        return dependencies.containsKey(key);
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
     * Collects extension properties from the {@code META-INF/quarkus-extension.properties}
     *
     * @param props extension properties
     * @param extensionKey extension dependency key
     */
    public void handleExtensionProperties(Properties props, ArtifactKey extensionKey) {
        JvmOptionsBuilder jvmOptionsBuilder = null;
        Set<String> lockJvmOptions = Set.of();
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            if (prop.getValue() == null) {
                continue;
            }
            final String name = prop.getKey().toString();
            final String value = prop.getValue().toString().trim();

            if (JvmOptionsBuilder.isExtensionDevModeJvmOptionProperty(name)) {
                log.debugf("Extension %s configures JVM option %s=%s in dev mode", extensionKey, name, value);
                if (jvmOptionsBuilder == null) {
                    jvmOptionsBuilder = JvmOptions.builder();
                }
                jvmOptionsBuilder.addFromQuarkusExtensionProperty(name, value);
                continue;
            }

            if (value.isBlank()) {
                continue;
            }
            switch (name) {
                case PARENT_FIRST_ARTIFACTS:
                    addParentFirstArtifacts(value);
                    break;
                case RUNNER_PARENT_FIRST_ARTIFACTS:
                    addRunnerParentFirstArtifacts(value);
                    break;
                case EXCLUDED_ARTIFACTS:
                    addExcludedArtifacts(extensionKey, value);
                    break;
                case LESSER_PRIORITY_ARTIFACTS:
                    addLesserPriorityArtifacts(extensionKey, value);
                    break;
                case BootstrapConstants.EXT_DEV_MODE_LOCK_XX_JVM_OPTIONS:
                case BootstrapConstants.EXT_DEV_MODE_LOCK_JVM_OPTIONS:
                    lockJvmOptions = splitByCommaAndAddAll(value, lockJvmOptions);
                    break;
                default:
                    if (name.startsWith(REMOVED_RESOURCES_DOT)) {
                        addRemovedResources(extensionKey, name, value);
                    }
            }
        }
        if (jvmOptionsBuilder != null || lockJvmOptions != null) {
            extensionDevConfig.add(new ExtensionDevModeConfig(extensionKey,
                    jvmOptionsBuilder == null ? JvmOptions.builder().build() : jvmOptionsBuilder.build(),
                    lockJvmOptions));
        }
    }

    private static Set<String> splitByCommaAndAddAll(String commaList, Set<String> set) {
        var arr = commaList.split(COMMA);
        if (arr.length == 0) {
            return set;
        }
        if (set.isEmpty()) {
            return Set.of(arr);
        }
        set = new HashSet<>(set);
        for (int i = 0; i < arr.length; ++i) {
            set.add(arr[i]);
        }
        return set;
    }

    private void addRemovedResources(ArtifactKey extension, String name, String value) {
        final String keyStr = name.substring(REMOVED_RESOURCES_DOT.length());
        if (keyStr.isBlank()) {
            return;
        }
        final ArtifactKey key;
        try {
            key = ArtifactKey.fromString(keyStr);
        } catch (IllegalArgumentException e) {
            log.warnf("Failed to parse artifact key %s in %s from descriptor of extension %s", keyStr, name, extension);
            return;
        }
        final Set<String> resources;
        final Collection<String> existingResources = excludedResources.get(key);
        if (existingResources == null || existingResources.isEmpty()) {
            resources = Set.of(value.split(COMMA));
        } else {
            final String[] split = value.split(COMMA);
            resources = new HashSet<>(existingResources.size() + split.length);
            resources.addAll(existingResources);
            resources.addAll(List.of(split));
        }
        log.debugf("Extension %s is excluding resources %s from artifact %s", extension, resources, key);
        excludedResources.put(key, resources);
    }

    private void addLesserPriorityArtifacts(ArtifactKey extension, String value) {
        for (String artifact : value.split(COMMA)) {
            lesserPriorityArtifacts.add(toArtifactKey(artifact));
            log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
        }
    }

    private void addExcludedArtifacts(ArtifactKey extension, String value) {
        for (String artifact : value.split(COMMA)) {
            excludedArtifacts.add(ArtifactCoordsPattern.of(artifact));
            log.debugf("Extension %s is excluding %s", extension, artifact);
        }
    }

    private void addRunnerParentFirstArtifacts(String value) {
        for (String artifact : value.split(COMMA)) {
            runnerParentFirstArtifacts.add(toArtifactKey(artifact));
        }
    }

    private void addParentFirstArtifacts(String value) {
        for (String artifact : value.split(COMMA)) {
            parentFirstArtifacts.add(toArtifactKey(artifact));
        }
    }

    private static GACT toArtifactKey(String artifact) {
        return new GACT(artifact.split(":"));
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
        final ArtifactCoordsPattern[] excludePatterns = excludedArtifacts.toArray(new ArtifactCoordsPattern[0]);
        for (ResolvedDependencyBuilder db : this.dependencies.values()) {
            if (!matches(db.getArtifactCoords(), excludePatterns)) {
                db.setDependencies(ensureNoMatches(db.getDependencies(), excludePatterns));
                result.add(db.build());
            }
        }
        return result;
    }

    private static boolean matches(ArtifactCoords coords, ArtifactCoordsPattern[] patterns) {
        for (int i = 0; i < patterns.length; ++i) {
            if (patterns[i].matches(coords)) {
                return true;
            }
        }
        return false;
    }

    private static Collection<ArtifactCoords> ensureNoMatches(Collection<ArtifactCoords> artifacts,
            ArtifactCoordsPattern[] patterns) {
        if (artifacts.isEmpty() || patterns.length == 0) {
            return artifacts;
        }
        for (var dep : artifacts) {
            if (matches(dep, patterns)) {
                return excludeMatches(artifacts, patterns);
            }
        }
        return artifacts;
    }

    private static Collection<ArtifactCoords> excludeMatches(Collection<ArtifactCoords> artifacts,
            ArtifactCoordsPattern[] patterns) {
        final List<ArtifactCoords> result = new ArrayList<>(artifacts.size() - 1);
        for (var artifact : artifacts) {
            if (!matches(artifact, patterns)) {
                result.add(artifact);
            }
        }
        return result;
    }

    public DefaultApplicationModel build() {
        return new DefaultApplicationModel(this);
    }
}
