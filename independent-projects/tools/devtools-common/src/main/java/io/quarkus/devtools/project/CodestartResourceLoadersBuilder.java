package io.quarkus.devtools.project;

import static io.quarkus.platform.catalog.processor.CatalogProcessor.getCodestartArtifacts;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getCodestartArtifact;
import static io.quarkus.platform.descriptor.loader.json.ResourceLoaders.resolveFileResourceLoader;
import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyNodeUtils;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.PropertiesUtil;
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

public final class CodestartResourceLoadersBuilder {
    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTY = "quarkus-base-codestart-artifact";
    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME = "/quarkus-devtools-base-codestarts.properties";

    private static final String BASE_CODESTARTS_ARTIFACT_COORDS = retrieveBaseCodestartsArtifactCoords();
    private ExtensionCatalog catalog = null;
    private MavenArtifactResolver artifactResolver = QuarkusProjectHelper.artifactResolver();
    private String baseCodestartsArtifactCoords = BASE_CODESTARTS_ARTIFACT_COORDS;
    private Collection<String> extraCodestartsArtifactCoords = new ArrayList<>();

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

    private CodestartResourceLoadersBuilder() {
    }

    public static CodestartResourceLoadersBuilder codestartLoadersBuilder() {
        return new CodestartResourceLoadersBuilder();
    }

    public static List<ResourceLoader> getCodestartResourceLoaders() {
        return codestartLoadersBuilder().build();
    }

    public static List<ResourceLoader> getCodestartResourceLoaders(ExtensionCatalog catalog) {
        return codestartLoadersBuilder().catalog(catalog).build();
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
                artifactResolver);
    }

    private static List<ResourceLoader> getCodestartResourceLoaders(String baseCodestartsArtifactCoords,
            Collection<String> extraCodestartsArtifactCoords,
            ExtensionCatalog catalog,
            MavenArtifactResolver mvn) {
        final Map<String, Artifact> codestartsArtifacts = new LinkedHashMap<>();

        if (catalog != null) {
            // Load codestarts from each extensions codestart artifacts
            for (Extension e : catalog.getExtensions()) {
                final String artifactCoords = getCodestartArtifact(e);
                if (artifactCoords == null || codestartsArtifacts.containsKey(artifactCoords)) {
                    continue;
                }
                codestartsArtifacts.put(artifactCoords, DependencyNodeUtils.toArtifact(artifactCoords));
            }
        }

        // Load base codestart artifacts
        if (baseCodestartsArtifactCoords != null) {
            codestartsArtifacts.put(baseCodestartsArtifactCoords, DependencyNodeUtils.toArtifact(baseCodestartsArtifactCoords));
        }

        if (catalog != null) {
            // Load codestarts from catalog codestart artifacts
            final List<String> catalogCodestartArtifacts = getCodestartArtifacts(catalog);
            for (String artifactCoords : catalogCodestartArtifacts) {
                if (codestartsArtifacts.containsKey(artifactCoords)) {
                    continue;
                }
                codestartsArtifacts.put(artifactCoords, DependencyNodeUtils.toArtifact(artifactCoords));
            }
        }

        // Load codestarts from the given artifacts
        for (String codestartArtifactCoords : extraCodestartsArtifactCoords) {
            codestartsArtifacts.put(codestartArtifactCoords, DependencyNodeUtils.toArtifact(codestartArtifactCoords));
        }

        final List<ResourceLoader> codestartResourceLoaders = new ArrayList<>(codestartsArtifacts.size());
        for (Artifact a : codestartsArtifacts.values()) {
            try {
                final File artifactFile = mvn.resolve(a).getArtifact().getFile();
                codestartResourceLoaders.add(resolveFileResourceLoader(artifactFile));
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve codestart artifact " + a, e);
            }
        }
        return codestartResourceLoaders;
    }
}
