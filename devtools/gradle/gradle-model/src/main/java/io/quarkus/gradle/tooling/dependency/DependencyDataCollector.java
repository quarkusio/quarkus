package io.quarkus.gradle.tooling.dependency;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.internal.composite.IncludedBuildInternal;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.gradle.tooling.GradleAssistedMavenModelResolverImpl;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.taskrunner.MavenModelResolutionTaskRunner;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class DependencyDataCollector {

    private static final String DISABLE_DECLARED_DEPENDENCY_COLLECTOR = "disableDeclaredDependencyCollector";

    private static final String SCOPE_RUNTIME = "runtime";
    private static final String SCOPE_TEST = "test";

    private final GradleAssistedMavenModelResolverImpl mavenModelResolver;
    private final DefaultModelBuilder modelBuilder;

    private final Map<DeclaredDepsCacheKey, DeclaredDepsResult> declaredDependenciesCache = new ConcurrentHashMap<>();

    public DependencyDataCollector(Project project) {
        this.mavenModelResolver = new GradleAssistedMavenModelResolverImpl(project);
        this.modelBuilder = new DefaultModelBuilderFactory().newInstance();
    }

    /**
     * Setting the project property to 'true' will disable declared dependency collection
     * for this project.
     */
    public static boolean isDisableDeclaredDependencyCollector(Project project) {
        final Object value = project.getProperties().get(DISABLE_DECLARED_DEPENDENCY_COLLECTOR);
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Sets direct dependencies for the given dependency builder based on the provided declared dependencies.
     * <p>
     * The intention here is to use this method right before we build the model, making sure that the modelBuilder
     * hass all the info about all dependencies, so we can properly set the flags for direct dependencies (e.g.
     * MISSING_FROM_APPLICATION).
     */
    public static void setDirectDeps(
            ResolvedDependencyBuilder depBuilder,
            ApplicationModelBuilder modelBuilder,
            Map<ArtifactKey, DeclaredDepsResult> declaredDependencies, Logger logger) {
        final DeclaredDepsResult declaredDepsResult = declaredDependencies.get(depBuilder.getKey());
        if (declaredDepsResult == null || !declaredDepsResult.isResolved()) {
            logger.info("Declared dependencies not found for {}", depBuilder.getArtifactCoords().toGACTVString());
            return;
        }
        final List<DependencyDataCollector.DeclaredDependency> declaredDeps = declaredDepsResult.getDeclaredDependencies();

        final List<io.quarkus.maven.dependency.Dependency> directDeps = new ArrayList<>(declaredDeps.size());
        final List<ArtifactCoords> depCoords = new ArrayList<>(declaredDeps.size());

        for (var declaredDep : declaredDeps) {
            var builder = DependencyBuilder.newInstance()
                    .setGroupId(declaredDep.getGroupId())
                    .setArtifactId(declaredDep.getArtifactId())
                    .setClassifier(defaultIfNull(declaredDep.getClassifier(), ArtifactCoords.DEFAULT_CLASSIFIER))
                    .setType(defaultIfNull(declaredDep.getType(), ArtifactCoords.TYPE_JAR))
                    .setVersion(declaredDep.getVersion());

            if (declaredDep.getScope() != null) {
                builder.setScope(declaredDep.getScope());
            }

            var appDep = modelBuilder.getDependency(builder.getKey());
            if (appDep == null) {
                builder.setFlags(DependencyFlags.MISSING_FROM_APPLICATION);
            } else {
                builder.setVersion(appDep.getVersion())
                        .setFlags(appDep.getFlags());
            }

            builder.setOptional(declaredDep.isOptional())
                    .setFlags(DependencyFlags.DIRECT);

            var directDep = builder.build();
            directDeps.add(directDep);

            if (appDep != null) {
                depCoords.add(toPlainArtifactCoords(directDep));
            }
        }

        depBuilder.setDependencies(depCoords)
                .setDirectDependencies(directDeps);
    }

    /**
     * Builds a deterministic snapshot of declared dependencies for task inputs.
     */
    public static List<String> toSnapshot(Map<ArtifactKey, DeclaredDepsResult> declaredDependencies) {
        if (declaredDependencies.isEmpty()) {
            return List.of();
        }
        final Map<String, DeclaredDepsResult> sorted = new TreeMap<>();
        for (var entry : declaredDependencies.entrySet()) {
            ArtifactKey key = entry.getKey();
            String keyString = key.getGroupId() + ":" + key.getArtifactId()
                    + ":" + defaultIfNull(key.getClassifier(), "")
                    + ":" + defaultIfNull(key.getType(), "");
            sorted.put(keyString, entry.getValue());
        }

        final List<String> snapshot = new ArrayList<>(sorted.size());
        for (var entry : sorted.entrySet()) {
            DeclaredDepsResult result = entry.getValue();
            snapshot.add("k:" + entry.getKey() + "|resolved=" + result.isResolved());
            List<String> deps = new ArrayList<>(result.getDeclaredDependencies().size());
            for (var dep : result.getDeclaredDependencies()) {
                deps.add("d:" + dep.getGroupId() + ":" + dep.getArtifactId()
                        + ":" + defaultIfNull(dep.getVersion(), "")
                        + ":" + defaultIfNull(dep.getClassifier(), "")
                        + ":" + defaultIfNull(dep.getType(), "")
                        + ":" + defaultIfNull(dep.getScope(), "")
                        + ":" + dep.isOptional());
            }
            Collections.sort(deps);
            snapshot.addAll(deps);
        }

        return List.copyOf(snapshot);
    }

    /**
     * Collects and returns declared dependencies for the given configuration.
     */
    public Map<ArtifactKey, DeclaredDepsResult> collectDeclaredDependencies(Project project, Configuration configuration) {
        if (isDisableDeclaredDependencyCollector(project)) {
            return Collections.emptyMap();
        }

        var startTime = project.getLogger().isDebugEnabled() ? System.currentTimeMillis() : -1;
        ArtifactCollection artifacts = configuration.getIncoming().getArtifacts();
        boolean isTestConfig = configuration.getName().toLowerCase().contains("test");
        MavenModelResolutionTaskRunner taskRunner = new MavenModelResolutionTaskRunner((task, error) -> project.getLogger()
                .error("Error during declared dependencies collection task execution: {}", error.getMessage(), error));
        Map<ArtifactKey, DeclaredDepsResult> result = new ConcurrentHashMap<>();
        collectDeclaredFromRootProject(project, isTestConfig, result);
        for (ResolvedArtifactResult artifact : artifacts.getArtifacts()) {
            var componentId = artifact.getId().getComponentIdentifier();
            if (componentId instanceof ModuleComponentIdentifier moduleId) {
                taskRunner.run(() -> collectDeclaredFromModule(project, artifact, moduleId, result));
            } else if (componentId instanceof ProjectComponentIdentifier projectId) {
                collectDeclaredFromNonRootProject(project, artifact, projectId, result);
            }
        }
        taskRunner.waitForCompletion();
        if (startTime > 0) {
            project.getLogger().debug("Declared dependencies collection for configuration {} took {} ms",
                    configuration.getName(), System.currentTimeMillis() - startTime);
        }
        return result;
    }

    private void collectDeclaredFromModule(
            Project project,
            ResolvedArtifactResult artifact,
            ModuleComponentIdentifier moduleId,
            Map<ArtifactKey, DeclaredDepsResult> resultMap) {

        String groupId = moduleId.getGroup();
        String artifactId = moduleId.getModule();
        String version = moduleId.getVersion();
        String type = resolveArtifactType(artifact);
        ArtifactKey moduleKey = DependencyUtils.getKey(groupId, artifactId, version, artifact.getFile(), type);
        DeclaredDepsResult result = declaredDependenciesCache.computeIfAbsent(new DeclaredDepsCacheKey(moduleKey, false),
                key -> {
                    try {
                        var modelSource = mavenModelResolver.resolveModel(moduleKey.getGroupId(), moduleKey.getArtifactId(),
                                version);
                        var request = new DefaultModelBuildingRequest();
                        request.setModelSource(modelSource);
                        request.setModelResolver(mavenModelResolver);
                        request.getSystemProperties().putAll(System.getProperties());
                        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                        Model effectiveModel = modelBuilder.build(request).getEffectiveModel();
                        List<DeclaredDependency> declaredDeps = toDeclaredDependencies(effectiveModel);
                        return DeclaredDepsResult.resolved(declaredDeps);
                    } catch (UnresolvableModelException | ModelBuildingException e) {
                        project.getLogger().warn("Unable to resolve effective model for {}:{}:{}: {}",
                                moduleKey.getGroupId(), moduleKey.getArtifactId(), version, e.getMessage());
                        return DeclaredDepsResult.unresolved();
                    }
                });
        resultMap.put(moduleKey, result);
    }

    private void collectDeclaredFromNonRootProject(
            Project project,
            ResolvedArtifactResult artifact,
            ProjectComponentIdentifier projectId,
            Map<ArtifactKey, DeclaredDepsResult> resultMap) {
        final Project depProject = getProject(project, projectId);
        if (depProject == null) {
            throw new GradleException("Project dependency not found for path: " + projectId.getProjectPath());
        }
        String groupId = String.valueOf(depProject.getGroup());
        String artifactId = depProject.getName();
        String version = String.valueOf(depProject.getVersion());
        String type = resolveArtifactType(artifact);
        ArtifactKey projectKey = DependencyUtils.getKey(groupId, artifactId, version, artifact.getFile(), type);
        // from this code branche, depProject is never a root project, so we set collectTestScopes to false
        DeclaredDepsResult result = declaredDependenciesCache.computeIfAbsent(new DeclaredDepsCacheKey(projectKey, false),
                key -> DeclaredDepsResult.resolved(collectDeclaredFromProject(depProject, false)));
        resultMap.put(projectKey, result);
    }

    private static Project getProject(Project project, ProjectComponentIdentifier projectId) {
        var includedBuild = ToolingUtils.includedBuild(project, projectId.getBuild().getBuildPath());
        final Project depProject;
        if (includedBuild != null) {
            if (includedBuild instanceof IncludedBuildInternal ib) {
                depProject = ToolingUtils.includedBuildProject(ib, projectId.getProjectPath());
            } else {
                depProject = null;
            }
        } else {
            depProject = project.getRootProject().findProject(projectId.getProjectPath());
        }
        return depProject;
    }

    private void collectDeclaredFromRootProject(Project project, boolean isTestConfig,
            Map<ArtifactKey, DeclaredDepsResult> resultMap) {
        String groupId = String.valueOf(project.getGroup());
        String artifactId = project.getName();
        ArtifactKey projectKey = ArtifactKey.of(groupId, artifactId,
                ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR);
        DeclaredDepsCacheKey cacheKey = new DeclaredDepsCacheKey(projectKey, isTestConfig);
        DeclaredDepsResult result = declaredDependenciesCache.computeIfAbsent(
                cacheKey,
                k -> DeclaredDepsResult.resolved(collectDeclaredFromProject(project, isTestConfig)));
        resultMap.put(projectKey, result);
    }

    private record DeclaredDepsCacheKey(ArtifactKey artifactKey, boolean includeTestScopes) {
    }

    private static List<DeclaredDependency> collectDeclaredFromProject(Project project,
            boolean collectTestScopes) {
        // Configuration to scope mapping:
        // api/implementation -> compile
        // runtimeOnly -> runtime
        // compileOnly -> ignored altogether
        // test* -> test
        final Map<GAV, DeclaredDependency> declaredDeps = new LinkedHashMap<>();

        addDeclaredFromConfig(project, JavaPlugin.API_CONFIGURATION_NAME,
                io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE, declaredDeps);
        addDeclaredFromConfig(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE, declaredDeps);
        addDeclaredFromConfig(project, JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, SCOPE_RUNTIME, declaredDeps);
        if (collectTestScopes) {
            addDeclaredFromConfig(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
            addDeclaredFromConfig(project, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
            // addDeclaredFromConfig(project, JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
        }

        return new ArrayList<>(declaredDeps.values());
    }

    private static void addDeclaredFromConfig(Project p, String cfgName, String scope,
            Map<GAV, DeclaredDependency> out) {
        final Configuration cfg = p.getConfigurations().findByName(cfgName);
        if (cfg == null) {
            return;
        }

        for (var d : cfg.getDependencies()) {
            var gav = new GAV(
                    String.valueOf(d.getGroup()),
                    d.getName(),
                    String.valueOf(d.getVersion()));
            if (d instanceof ProjectDependency pd) {
                Project dp = p.findProject(pd.getPath());
                if (dp == null) {
                    // should not happen
                    throw new GradleException("Failed to find project for dependency: " + pd.getPath());
                }
            }
            out.put(gav, new DeclaredDependency(
                    gav.getGroupId(),
                    gav.getArtifactId(),
                    gav.getVersion(),
                    null,
                    null,
                    scope,
                    false));

        }
    }

    private static List<DeclaredDependency> toDeclaredDependencies(Model model) {
        final List<DeclaredDependency> declaredDeps = new ArrayList<>();
        for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
            if (!SCOPE_TEST.equals(dep.getScope())) {
                declaredDeps.add(new DeclaredDependency(dep));
            }
        }
        return declaredDeps;
    }

    private static String resolveArtifactType(ResolvedArtifactResult artifact) {
        return artifact.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
    }

    private static ArtifactCoords toPlainArtifactCoords(io.quarkus.maven.dependency.Dependency dep) {
        return ArtifactCoords.of(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion());
    }

    private static String defaultIfNull(String value, String fallback) {
        return value == null ? fallback : value;
    }

    public static class DeclaredDependency implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String type;
        private final String version;
        private final String scope;
        private final boolean optional;

        DeclaredDependency(org.apache.maven.model.Dependency dep) {
            this.groupId = dep.getGroupId();
            this.artifactId = dep.getArtifactId();
            this.classifier = defaultIfNull(dep.getClassifier(), ArtifactCoords.DEFAULT_CLASSIFIER);
            this.type = defaultIfNull(dep.getType(), ArtifactCoords.TYPE_JAR);
            this.version = dep.getVersion();
            this.scope = defaultIfNull(dep.getScope(), io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE);
            this.optional = Boolean.parseBoolean(dep.getOptional());
        }

        DeclaredDependency(String groupId, String artifactId, String version,
                String classifier, String type, String scope, boolean optional) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
            this.scope = scope;
            this.optional = optional;
        }

        String getGroupId() {
            return groupId;
        }

        String getArtifactId() {
            return artifactId;
        }

        String getClassifier() {
            return classifier;
        }

        String getType() {
            return type;
        }

        String getVersion() {
            return version;
        }

        String getScope() {
            return scope;
        }

        boolean isOptional() {
            return optional;
        }
    }

    public static class DeclaredDepsResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final List<DeclaredDependency> declaredDependencies;
        private final boolean resolved;

        private DeclaredDepsResult(List<DeclaredDependency> declaredDependencies, boolean resolved) {
            this.declaredDependencies = declaredDependencies;
            this.resolved = resolved;
        }

        public static DeclaredDepsResult resolved(List<DeclaredDependency> declaredDependencies) {
            return new DeclaredDepsResult(declaredDependencies, true);
        }

        public static DeclaredDepsResult unresolved() {
            return new DeclaredDepsResult(List.of(), false);
        }

        public List<DeclaredDependency> getDeclaredDependencies() {
            return declaredDependencies;
        }

        public boolean isResolved() {
            return resolved;
        }
    }

}
