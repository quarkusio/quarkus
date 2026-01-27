package io.quarkus.gradle.tooling;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class DependencyInfoCollector {

    private static final String SCOPE_RUNTIME = "runtime";
    private static final String SCOPE_TEST = "test";

    private final Map<ArtifactKey, List<DeclaredDependency>> declaredDepsByArtifactKey = new HashMap<>();
    private final Map<GAV, Model> pomModelByGav;
    private final Map<String, List<DeclaredDependency>> declaredDepsByProjectPath;
    private final Logger logger;

    public DependencyInfoCollector(Map<GAV, Model> pomModelByGav,
            Map<String, List<DeclaredDependency>> declaredDepsByProjectPath,
            Logger logger) {
        this.pomModelByGav = pomModelByGav;
        this.declaredDepsByProjectPath = declaredDepsByProjectPath;
        this.logger = logger;
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

    public void collect(ComponentIdentifier componentId, GAV gav, ResolvedDependencyBuilder depBuilder) {
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
            final List<DeclaredDependency> declared = declaredDepsByProjectPath.get(compId.getProjectPath());
            final List<DeclaredDependency> filtered = filterTestScopes(declared);
            if (filtered != null) {
                declaredDepsByArtifactKey.put(artifactKey, filtered);
            }
            return;
        }

        // External module -> collect via POM resolution
        final Model model = pomModelByGav.get(gav);
        if (model != null) {
            final List<DeclaredDependency> declared = model.getDependencies().stream()
                    // all test scope dependencies are ignored for module components
                    .filter(dep -> !SCOPE_TEST.equals(dep.getScope()))
                    .map(DeclaredDependency::new)
                    .toList();
            declaredDepsByArtifactKey.put(artifactKey, declared);
        }
    }

    public void collectProjectArtifact(ArtifactKey appKey, List<DeclaredDependency> declaredDeps, boolean includeTestScopes) {
        if (declaredDeps == null) {
            return;
        }
        declaredDepsByArtifactKey.put(appKey, includeTestScopes ? declaredDeps : filterTestScopes(declaredDeps));
    }

    public List<DeclaredDependency> get(ArtifactKey artifactKey) {
        return declaredDepsByArtifactKey.get(artifactKey);
    }

    static List<DeclaredDependency> collectDeclaredFromProject(Project project, boolean collectTestScopes) {
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

    private static void addDeclaredFromConfig(Project p, String cfgName, String scope, List<DeclaredDependency> out) {
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
        if (declaredDeps == null) {
            return null;
        }
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

    // private Model readModel(GAV gav) {
    //     File pomFile = pomFilesByGav.get(gav);
    //     if (pomFile == null) {
    //         return null;
    //     }
    //     MavenXpp3Reader reader = new MavenXpp3Reader();
    //     try (var input = Files.newBufferedReader(pomFile.toPath(), StandardCharsets.UTF_8)) {
    //         return reader.read(input);
    //     } catch (IOException | XmlPullParserException e) {
    //         logger.info("Failed to read Maven POM {}:{}:{}: {}",
    //                 gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), e.getMessage());
    //         return null;
    //     }
    // }

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
