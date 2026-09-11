package io.quarkus.gradle.application.internal.plugin;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import io.quarkus.gradle.model.pom.ExternalModuleDeclaredDependencyInput;
import io.quarkus.gradle.model.pom.PomClosureResult;
import io.quarkus.gradle.model.pom.PomClosureTaskInput;
import io.quarkus.gradle.model.pom.StrictDependencyDataCollector;
import io.quarkus.gradle.model.tasks.GradlePomClosureResolver;
import io.quarkus.maven.dependency.GAV;

final class PomClosureInputCalculator implements Callable<PomClosureTaskInput> {

    private static final Logger LOG = Logging.getLogger(PomClosureInputCalculator.class);

    private final DependencyHandler dependencies;
    private final ProviderFactory providers;
    private final Provider<List<ExternalModuleDeclaredDependencyInput>> externalModuleInputs;
    private final Provider<Map<String, String>> selectedPomFilesByGav;
    private final Provider<List<String>> mavenLocalRepositoryRoots;

    PomClosureInputCalculator(DependencyHandler dependencies, ProviderFactory providers,
            Provider<List<ExternalModuleDeclaredDependencyInput>> externalModuleInputs,
            Provider<Map<String, String>> selectedPomFilesByGav,
            Provider<List<String>> mavenLocalRepositoryRoots) {
        this.dependencies = dependencies;
        this.providers = providers;
        this.externalModuleInputs = externalModuleInputs;
        this.selectedPomFilesByGav = selectedPomFilesByGav;
        this.mavenLocalRepositoryRoots = mavenLocalRepositoryRoots;
    }

    @Override
    public PomClosureTaskInput call() {
        var pomResolver = GradlePomClosureResolver.withGradleArtifactResolution(
                selectedPomFilesByGav(), dependencies, mavenLocalRepositoryRoots());
        var collector = new StrictDependencyDataCollector(pomResolver,
                new ReferencedSystemProperties(providers, pomResolver));
        collector.collectExternalDeclaredDependencies(LOG, externalModuleInputs.get());
        return PomClosureTaskInput.from(PomClosureResult.from(pomResolver.getPomResults()));
    }

    private Map<GAV, File> selectedPomFilesByGav() {
        Map<GAV, File> result = new TreeMap<>(Comparator.comparing(GAV::toString));
        selectedPomFilesByGav.get().forEach((gav, file) -> result.put(parseGav(gav), new File(file)));
        return result;
    }

    private Collection<File> mavenLocalRepositoryRoots() {
        return mavenLocalRepositoryRoots.get().stream()
                .filter(root -> !root.isBlank())
                .map(File::new)
                .toList();
    }

    private static GAV parseGav(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("POM closure GAV must have format groupId:artifactId:version: " + value);
        }
        return new GAV(parts[0], parts[1], parts[2]);
    }

    /**
     * Limits configuration-cache system-property inputs to names that a resolved
     * POM actually references for interpolation or profile activation. Each
     * discovered property is queried once for the lifetime of the closure calculation.
     */
    static final class ReferencedSystemProperties implements Supplier<Map<String, String>> {
        private static final Pattern PROPERTY_EXPRESSION = Pattern.compile("\\$\\{([^{}]+)}");

        private final ProviderFactory providers;
        private final GradlePomClosureResolver pomResolver;
        private final Set<File> inspectedPomFiles = new HashSet<>();
        private final Set<String> referencedPropertyNames = new HashSet<>();
        private final Set<String> queriedPropertyNames = new HashSet<>();
        private final Map<String, String> referencedPropertyValues = new HashMap<>();
        private Map<String, String> referencedPropertyValuesSnapshot = Map.of();

        ReferencedSystemProperties(ProviderFactory providers, GradlePomClosureResolver pomResolver) {
            this.providers = providers;
            this.pomResolver = pomResolver;
        }

        @Override
        public Map<String, String> get() {
            for (Optional<File> pomFile : pomResolver.getPomResults().values()) {
                if (pomFile.isEmpty()) {
                    continue;
                }
                File file = pomFile.get();
                if (inspectedPomFiles.contains(file)) {
                    continue;
                }
                collectReferencedPropertyNames(file, referencedPropertyNames);
                inspectedPomFiles.add(file);
            }
            boolean valuesChanged = false;
            for (String name : referencedPropertyNames) {
                if (queriedPropertyNames.contains(name)) {
                    continue;
                }
                String value = providers.systemProperty(name).getOrNull();
                queriedPropertyNames.add(name);
                if (value != null) {
                    referencedPropertyValues.put(name, value);
                    valuesChanged = true;
                }
            }
            if (valuesChanged) {
                referencedPropertyValuesSnapshot = Map.copyOf(referencedPropertyValues);
            }
            return referencedPropertyValuesSnapshot;
        }

        private static void collectReferencedPropertyNames(File pomFile, Set<String> target) {
            try {
                String pom = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
                Matcher matcher = PROPERTY_EXPRESSION.matcher(pom);
                while (matcher.find()) {
                    addPropertyName(matcher.group(1), target);
                }
                try (var reader = new StringReader(pom)) {
                    for (Profile profile : new MavenXpp3Reader().read(reader).getProfiles()) {
                        if (profile.getActivation() != null && profile.getActivation().getProperty() != null) {
                            addPropertyName(profile.getActivation().getProperty().getName(), target);
                        }
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to inspect POM system-property references in " + pomFile, e);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse POM system-property references in " + pomFile, e);
            }
        }

        private static void addPropertyName(String value, Set<String> target) {
            if (value == null) {
                return;
            }
            String name = value.trim();
            if (name.startsWith("!")) {
                name = name.substring(1);
            }
            if (!name.isBlank()) {
                target.add(name);
            }
        }
    }
}
