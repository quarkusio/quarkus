package io.quarkus.devtools.project;

import static io.quarkus.platform.catalog.processor.CatalogProcessor.getCodestartArtifacts;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getCodestartArtifact;
import static io.quarkus.platform.descriptor.loader.json.ResourceLoaders.resolveFileResourceLoader;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.PropertiesUtil;

public final class CodestartResourceLoadersBuilder {
    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTY = "quarkus-base-codestart-artifact";
    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME = "/quarkus-devtools-base-codestarts.properties";

    private static final String BASE_CODESTARTS_ARTIFACT_COORDS = retrieveBaseCodestartsArtifactCoords();
    private ExtensionCatalog catalog = null;
    private MavenArtifactResolver artifactResolver;
    private String baseCodestartsArtifactCoords = BASE_CODESTARTS_ARTIFACT_COORDS;
    private Collection<String> extraCodestartsArtifactCoords = new ArrayList<>();
    private MessageWriter log;

    private static String retrieveBaseCodestartsArtifactCoords() {
        final String artifact = PropertiesUtil.getProperty(BASE_CODESTARTS_ARTIFACT_PROPERTY);
        if (artifact != null) {
            return artifact;
        }
        try (final InputStream resource = QuarkusProjectHelper.class
                .getResourceAsStream(BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME)) {
            final Properties properties = new Properties();
            requireNonNull(resource,
                    BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME + " resource not found.");
            properties.load(resource);
            return requireNonNull(properties.getProperty("artifact"),
                    "base codestarts 'artifact' property not found");
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load the base codestarts artifact properties", e);
        }
    }

    private CodestartResourceLoadersBuilder(MessageWriter log) {
        this.log = log;
    }

    public static CodestartResourceLoadersBuilder codestartLoadersBuilder(MessageWriter log) {
        return new CodestartResourceLoadersBuilder(log);
    }

    public static List<ResourceLoader> getCodestartResourceLoaders(MessageWriter log) {
        return codestartLoadersBuilder(log).build();
    }

    public static List<ResourceLoader> getCodestartResourceLoaders(MessageWriter log, ExtensionCatalog catalog) {
        return codestartLoadersBuilder(log).catalog(catalog).build();
    }

    public CodestartResourceLoadersBuilder catalog(ExtensionCatalog catalog) {
        this.catalog = catalog;
        return this;
    }

    public CodestartResourceLoadersBuilder artifactResolver(MavenArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public CodestartResourceLoadersBuilder baseCodestartsArtifactCoords(String baseCodestartsArtifactCoords) {
        this.baseCodestartsArtifactCoords = baseCodestartsArtifactCoords;
        return this;
    }

    public CodestartResourceLoadersBuilder noBaseCodestarts() {
        this.baseCodestartsArtifactCoords = null;
        return this;
    }

    public CodestartResourceLoadersBuilder addExtraCodestartsArtifactCoords(Collection<String> extraCodestartsArtifactCoords) {
        this.extraCodestartsArtifactCoords.addAll(extraCodestartsArtifactCoords);
        return this;
    }

    public List<ResourceLoader> build() {
        return getCodestartResourceLoaders(baseCodestartsArtifactCoords, extraCodestartsArtifactCoords, catalog,
                artifactResolver == null ? QuarkusProjectHelper.artifactResolver() : artifactResolver, log);
    }

    private static List<ResourceLoader> getCodestartResourceLoaders(String baseCodestartsArtifactCoords,
            Collection<String> extraCodestartsArtifactCoords,
            ExtensionCatalog catalog,
            MavenArtifactResolver mavenArtifactResolver,
            MessageWriter log) {

        final Map<String, Artifact> codestartsArtifacts = new LinkedHashMap<>();

        // The latest inserted in the Map will have priority over the previous (in case of codestarts name conflicts)
        // We have to remove keys to override because 'put' keeps the order in a LinkedHashMap
        if (catalog != null) {
            // Load codestarts from each extensions codestart artifacts
            for (Extension e : catalog.getExtensions()) {
                final String coords = getCodestartArtifact(e);
                if (coords == null || codestartsArtifacts.containsKey(coords)) {
                    continue;
                }
                codestartsArtifacts.put(coords, DependencyUtils.toArtifact(coords));
            }
        }

        // Load base codestart artifacts
        if (baseCodestartsArtifactCoords != null) {
            codestartsArtifacts.put(baseCodestartsArtifactCoords, DependencyUtils.toArtifact(baseCodestartsArtifactCoords));
        }

        if (catalog != null) {
            // Load codestarts from catalog codestart artifacts
            final List<String> catalogCodestartArtifacts = getCodestartArtifacts(catalog);
            for (String coords : catalogCodestartArtifacts) {
                if (codestartsArtifacts.containsKey(coords)) {
                    // Make sure it overrides the previous codestarts
                    codestartsArtifacts.remove(coords);
                }
                codestartsArtifacts.put(coords, DependencyUtils.toArtifact(coords));
            }
        }

        // Load codestarts from the given artifacts
        for (String coords : extraCodestartsArtifactCoords) {
            if (codestartsArtifacts.containsKey(coords)) {
                // Make sure it overrides the previous codestarts
                codestartsArtifacts.remove(coords);
            }
            codestartsArtifacts.put(coords, DependencyUtils.toArtifact(coords));
        }

        final List<ResourceLoader> codestartResourceLoaders = new ArrayList<>(codestartsArtifacts.size());
        for (Artifact a : codestartsArtifacts.values()) {
            try {
                final File artifactFile = mavenArtifactResolver.resolve(a).getArtifact().getFile();
                codestartResourceLoaders.add(resolveFileResourceLoader(artifactFile));
            } catch (Exception e) {
                log.warn("Unable to resolve codestart artifact for %s: %s", a, e.getMessage());
                continue;
            }
        }
        return codestartResourceLoaders;
    }
}
