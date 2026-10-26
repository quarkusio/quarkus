package io.quarkus.gradle.model.pom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class StrictDependencyDataCollector {

    private static final String SCOPE_TEST = "test";

    private final PomResolver pomResolver;
    private final Supplier<Map<String, String>> systemProperties;
    private final Map<DeclaredDepsCacheKey, DeclaredDepsResult> declaredDependenciesCache = new ConcurrentHashMap<>();

    public StrictDependencyDataCollector(PomResolver pomResolver, Supplier<Map<String, String>> systemProperties) {
        this.pomResolver = Objects.requireNonNull(pomResolver, "pomResolver cannot be null");
        this.systemProperties = Objects.requireNonNull(systemProperties, "systemProperties cannot be null");
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
        final List<DeclaredDependency> declaredDeps = declaredDepsResult.getDeclaredDependencies();

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

    public static List<ExternalModuleDeclaredDependencyInput> externalModuleDeclaredDependencyInputs(
            Collection<ResolvedArtifactResult> artifacts) {
        List<ExternalModuleDeclaredDependencyInput> moduleInputs = new ArrayList<>();
        for (ResolvedArtifactResult artifact : artifacts) {
            var componentId = artifact.getId().getComponentIdentifier();
            if (componentId instanceof ModuleComponentIdentifier moduleId) {
                moduleInputs.add(ExternalModuleDeclaredDependencyInput.from(artifact, moduleId));
            }
        }
        moduleInputs.sort(ExternalModuleDeclaredDependencyInput::compareTo);
        return List.copyOf(moduleInputs);
    }

    public Map<ArtifactKey, DeclaredDepsResult> collectExternalDeclaredDependencies(Logger logger,
            List<ExternalModuleDeclaredDependencyInput> moduleInputs) {
        Map<ArtifactKey, DeclaredDepsResult> result = new ConcurrentHashMap<>();
        collectDeclaredDependencies(logger, moduleInputs, result);
        return result;
    }

    private void collectDeclaredDependencies(Logger logger,
            List<ExternalModuleDeclaredDependencyInput> moduleInputs,
            Map<ArtifactKey, DeclaredDepsResult> result) {
        collectDeclaredDependenciesWithPomResolution(logger, moduleInputs, result);
    }

    /**
     * Builds Maven effective models from Gradle-resolved POMs and records their declared dependency edges.
     * <p>
     * The first iteration prefetches the POMs for the modules selected by Gradle. Maven model building may then request
     * parent POMs or imported BOM POMs that were not part of that initial set. Those requests are recorded, prefetched
     * through Gradle as a batch, and retried until no new POMs are discovered. Missing POMs are cached as unresolved
     * results so the loop terminates without repeated resolution attempts.
     * <p>
     * This intentionally follows Gradle's resolved module graph for the modules being enriched, while using Maven model
     * parsing only to recover information Gradle does not expose directly: which dependencies were declared by a POM, with
     * their Maven scopes and optional markers. Consumer-side Gradle constraints, platforms, and conflict resolution are
     * applied later when {@link #setDirectDeps(ResolvedDependencyBuilder, ApplicationModelBuilder, Map, Logger)} maps the
     * declared edges back onto the Gradle-built application model.
     */
    private void collectDeclaredDependenciesWithPomResolution(
            Logger logger,
            List<ExternalModuleDeclaredDependencyInput> moduleInputs,
            Map<ArtifactKey, DeclaredDepsResult> result) {
        pomResolver.prefetchPoms(moduleInputs.stream()
                .map(ExternalModuleDeclaredDependencyInput::getPomGav)
                .toList());

        List<ExternalModuleDeclaredDependencyInput> pending = new ArrayList<>();
        for (ExternalModuleDeclaredDependencyInput moduleInput : moduleInputs) {
            DeclaredDepsResult cached = declaredDependenciesCache.get(declaredDepsCacheKey(moduleInput));
            if (cached == null) {
                pending.add(moduleInput);
            } else {
                result.put(moduleInput.getArtifactKey(), cached);
            }
        }

        while (!pending.isEmpty()) {
            Set<GAV> missingPoms = new LinkedHashSet<>();
            List<ExternalModuleDeclaredDependencyInput> retry = new ArrayList<>();

            for (ExternalModuleDeclaredDependencyInput moduleInput : pending) {
                DeclaredDepsResult resolved = collectDeclaredFromModule(logger, moduleInput, missingPoms);
                if (resolved == null) {
                    retry.add(moduleInput);
                } else {
                    declaredDependenciesCache.putIfAbsent(declaredDepsCacheKey(moduleInput), resolved);
                    result.put(moduleInput.getArtifactKey(), resolved);
                }
            }

            if (missingPoms.isEmpty()) {
                for (ExternalModuleDeclaredDependencyInput moduleInput : retry) {
                    DeclaredDepsResult unresolved = DeclaredDepsResult.unresolved();
                    declaredDependenciesCache.putIfAbsent(declaredDepsCacheKey(moduleInput), unresolved);
                    result.put(moduleInput.getArtifactKey(), unresolved);
                }
                return;
            }

            pomResolver.prefetchPoms(missingPoms);
            pending = retry;
        }
    }

    private DeclaredDepsResult collectDeclaredFromModule(
            Logger logger,
            ExternalModuleDeclaredDependencyInput moduleInput,
            Set<GAV> missingPoms) {
        GAV pomGav = moduleInput.getPomGav();
        var recordingPomResolver = new RecordingPomResolver(pomResolver);
        try {
            var effectiveModelResolver = new MavenEffectiveModelResolver(recordingPomResolver, systemProperties);
            var effectiveModel = effectiveModelResolver.resolveEffectiveModel(
                    pomGav.getGroupId(), pomGav.getArtifactId(), pomGav.getVersion());
            return DeclaredDepsResult.resolved(toDeclaredDependencies(effectiveModel));
        } catch (UnresolvableModelException | ModelBuildingException e) {
            Set<GAV> requestedPoms = recordingPomResolver.getRequestedPoms();
            if (!requestedPoms.isEmpty()) {
                missingPoms.addAll(requestedPoms);
                return null;
            }
            logger.warn("Unable to resolve effective model for {}:{}:{}: {}",
                    pomGav.getGroupId(), pomGav.getArtifactId(), pomGav.getVersion(), e.getMessage());
            return DeclaredDepsResult.unresolved();
        }
    }

    private static DeclaredDepsCacheKey declaredDepsCacheKey(ExternalModuleDeclaredDependencyInput moduleInput) {
        return new DeclaredDepsCacheKey(moduleInput.getArtifactKey(), moduleInput.getPomGav(), false);
    }

    private record DeclaredDepsCacheKey(ArtifactKey artifactKey, GAV pomGav, boolean includeTestScopes) {
    }

    private static final class RecordingPomResolver implements PomResolver {

        private final PomResolver delegate;
        private final Set<GAV> requestedPoms = new LinkedHashSet<>();

        private RecordingPomResolver(PomResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
            if (!delegate.hasPomResult(gav)) {
                requestedPoms.add(gav);
                throw new UnresolvableModelException("POM not prefetched for " + gav,
                        gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
            }
            return delegate.resolvePom(gav);
        }

        private Set<GAV> getRequestedPoms() {
            return requestedPoms;
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

    static String resolveArtifactType(ResolvedArtifactResult artifact) {
        return artifact.getVariant().getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
    }

    static ArtifactKey resolveArtifactKey(ResolvedArtifactResult artifact, ModuleComponentIdentifier moduleId) {
        return ArtifactKey.of(moduleId.getGroup(), moduleId.getModule(),
                resolveClassifier(moduleId.getModule(), moduleId.getVersion(), artifact.getFile()),
                resolveArtifactType(artifact));
    }

    private static String resolveClassifier(String artifactId, String version, java.io.File file) {
        String artifactIdVersion = version == null || version.isEmpty() || "unspecified".equals(version)
                ? artifactId
                : artifactId + "-" + version;
        if ((file.getName().endsWith(".jar") || file.getName().endsWith(".pom") || file.getName().endsWith(".exe"))
                && file.getName().startsWith(artifactIdVersion + "-")) {
            return file.getName().substring(artifactIdVersion.length() + 1, file.getName().length() - 4);
        }
        return "";
    }

    private static ArtifactCoords toPlainArtifactCoords(io.quarkus.maven.dependency.Dependency dep) {
        return ArtifactCoords.of(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion());
    }

    static String defaultIfNull(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
