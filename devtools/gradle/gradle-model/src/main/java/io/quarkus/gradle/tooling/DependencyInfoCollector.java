package io.quarkus.gradle.tooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.internal.composite.IncludedBuildInternal;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.runtime.LaunchMode;

public class DependencyInfoCollector {

    private static final String SCOPE_RUNTIME = "runtime";
    private static final String SCOPE_TEST = "test";

    private final Map<ArtifactKey, List<DeclaredDependency>> declaredDepsByArtifactKey = new HashMap<>();
    private final GradleAssistedMavenModelResolverImpl modelResolver;
    private final DefaultModelBuilder builder;
    private final Logger logger;

    public DependencyInfoCollector(Project project) {
        this.modelResolver = new GradleAssistedMavenModelResolverImpl(project);
        builder = new DefaultModelBuilderFactory().newInstance();
        logger = project.getLogger();
    }

    public void setDirectDeps(
            ResolvedDependencyBuilder depBuilder,
            ApplicationModelBuilder modelBuilder) {
        final List<DeclaredDependency> declaredDeps = declaredDepsByArtifactKey.get(depBuilder.getKey());
        if (declaredDeps == null) {
            logger.info("Declared dependencies not found for {}", depBuilder.getArtifactCoords().toGACTVString());
            return;
        }

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

    public void collect(ComponentIdentifier componentId, GAV gav, ResolvedDependencyBuilder depBuilder, Project rootProject) {
        if (depBuilder == null) {
            return;
        }

        // We already collected it
        final ArtifactKey artifactKey = depBuilder.getKey();
        if (declaredDepsByArtifactKey.containsKey(artifactKey)) {
            return;
        }

        // Project component -> declared deps must come from the target Gradle project
        if (componentId instanceof ProjectComponentIdentifier compId) {
            final Project targetProject = resolveProjectComponent(rootProject, compId);
            final List<DeclaredDependency> declared = collectDeclaredFromProject(targetProject, false);
            declaredDepsByArtifactKey.put(artifactKey, declared);
            return;
        }

        // External module -> collect via POM resolution
        final Model model = resolvePomAndBuildModel(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), modelResolver);
        if (model != null) {
            final List<DeclaredDependency> declared = model.getDependencies().stream()
                    // all test scope dependencies are ignored for module components
                    .filter(dep -> !SCOPE_TEST.equals(dep.getScope()))
                    .map(DeclaredDependency::new)
                    .toList();
            declaredDepsByArtifactKey.put(artifactKey, declared);
        }
    }

    public void collectProjectArtifact(ArtifactKey appKey, Project appProject, LaunchMode mode) {
        List<DeclaredDependency> declaredDeps = collectDeclaredFromProject(appProject, LaunchMode.TEST.equals(mode));
        declaredDepsByArtifactKey.put(appKey, declaredDeps);
    }

    public List<DeclaredDependency> get(ArtifactKey artifactKey) {
        return declaredDepsByArtifactKey.get(artifactKey);
    }

    private Project resolveProjectComponent(Project rootProject, ProjectComponentIdentifier compId) {
        Project p = rootProject.getRootProject().findProject(compId.getProjectPath());
        if (p != null) {
            return p;
        }

        final IncludedBuild includedBuild = ToolingUtils.includedBuild(
                rootProject.getRootProject(),
                compId.getBuild().getBuildPath());
        if (includedBuild instanceof IncludedBuildInternal ib) {
            return ToolingUtils.includedBuildProject(ib, compId.getProjectPath());
        }
        throw new GradleException("Failed to resolve project component for " + compId.getDisplayName());
    }

    private List<DeclaredDependency> collectDeclaredFromProject(Project project, boolean collectTestScopes) {
        // Configuration to scope mapping:
        // api/implementation -> compile
        // runtimeOnly -> runtime
        // compileOnly -> ignored altogether
        // test* -> test
        final List<DeclaredDependency> declaredDeps = new ArrayList<>();

        addDeclaredFromConfig(project, JavaPlugin.API_CONFIGURATION_NAME,
                io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE, declaredDeps);
        addDeclaredFromConfig(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE, declaredDeps);
        addDeclaredFromConfig(project, JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, SCOPE_RUNTIME, declaredDeps);
        if (collectTestScopes) {
            addDeclaredFromConfig(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
            addDeclaredFromConfig(project, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
            addDeclaredFromConfig(project, JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, SCOPE_TEST, declaredDeps);
        }

        return deduplicate(declaredDeps);
    }

    private void addDeclaredFromConfig(Project p, String cfgName, String scope, List<DeclaredDependency> out) {
        final Configuration cfg = p.getConfigurations().findByName(cfgName);
        if (cfg == null) {
            return;
        }

        for (var d : cfg.getDependencies()) {
            if (d instanceof ExternalModuleDependency emd) {
                out.add(new DeclaredDependency(
                        emd.getGroup(),
                        emd.getName(),
                        emd.getVersion(),
                        null, // TODO: clarify
                        null, // TODO: clarify
                        scope,
                        false));
                continue;
            }
            if (d instanceof ProjectDependency pd) {
                Project dp = p.findProject(pd.getPath());
                if (dp == null) {
                    // should not happen
                    throw new GradleException("Failed to find project for dependency: " + pd.getPath());
                }
                out.add(new DeclaredDependency(
                        String.valueOf(dp.getGroup()),
                        dp.getName(),
                        String.valueOf(dp.getVersion()), // TODO: this may have value "unspecified"
                        null, // TODO: clarify
                        null, // TODO: clarify
                        scope,
                        false));
            }
        }
    }

    private static List<DeclaredDependency> deduplicate(List<DeclaredDependency> in) {
        final Set<String> seen = new LinkedHashSet<>();
        final List<DeclaredDependency> out = new ArrayList<>(in.size());
        for (var d : in) {
            final String key = d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion()
                    + ":" + d.getClassifier() + ":" + d.getType() + ":" + d.getScope() + ":" + d.isOptional();
            if (seen.add(key)) {
                out.add(d);
            }
        }
        return out;
    }

    private static List<DeclaredDependency> filterTestScopes(List<DeclaredDependency> declaredDeps) {
        final List<DeclaredDependency> out = new ArrayList<>(declaredDeps.size());
        for (var dep : declaredDeps) {
            if (!SCOPE_TEST.equals(dep.getScope())) {
                out.add(dep);
            }
        }
        return out;
    }

    private static ArtifactCoords toPlainArtifactCoords(io.quarkus.maven.dependency.Dependency dep) {
        return ArtifactCoords.of(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion());
    }

    private static String defaultIfNull(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private Model resolvePomAndBuildModel(String groupId, String artifactId, String version,
            GradleAssistedMavenModelResolverImpl modelResolver) {
        try {
            // build the effective model
            return buildEffectiveModel(groupId, artifactId, version, modelResolver);
        } catch (Exception e) {
            logger.info("Failed to resolve POM for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
            return null;
        }
    }

    private Model buildEffectiveModel(String groupId, String artifactId, String version,
            GradleAssistedMavenModelResolverImpl modelResolver) {
        try {
            // resolve the POM to get the file
            var modelSource = modelResolver.resolveModel(groupId, artifactId, version);

            ModelBuildingRequest request = new DefaultModelBuildingRequest();
            request.setModelSource(modelSource);
            request.setModelResolver(modelResolver);
            request.getSystemProperties().putAll(System.getProperties());
            request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

            return builder.build(request).getEffectiveModel();
        } catch (Exception e) {
            logger.info("Failed to build Maven model for {}:{}:{}: {}",
                    groupId, artifactId, version, e.getMessage());
            return null;
        }
    }

    public static class DeclaredDependency {

        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String type;
        private final String version;
        private final String scope;
        private final boolean optional;

        DeclaredDependency(Dependency dep) {
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

}
