package io.quarkus.devtools.testing.codestarts;

import static io.quarkus.devtools.testing.RegistryClientTestHelper.disableRegistryClientTestConfig;
import static io.quarkus.devtools.testing.RegistryClientTestHelper.enableRegistryClientTestConfig;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getBuiltWithQuarkusCore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.platform.descriptor.loader.json.ResourceLoaders;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class QuarkusCodestartTestBuilder {
    public Map<String, Object> data = new HashMap<>();
    public Collection<String> artifacts = new ArrayList<>();
    BuildTool buildTool;
    Set<String> codestarts = new HashSet<>();
    Set<Language> languages;
    QuarkusCodestartCatalog quarkusCodestartCatalog;
    ExtensionCatalog extensionCatalog;
    Collection<ArtifactCoords> extensions = new ArrayList<>();

    public QuarkusCodestartTestBuilder codestarts(String... codestarts) {
        this.codestarts = new HashSet<>(Arrays.asList(codestarts));
        return this;
    }

    public QuarkusCodestartTestBuilder languages(Language... languages) {
        this.languages = new HashSet<>(Arrays.asList(languages));
        return this;
    }

    public QuarkusCodestartTestBuilder buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    public QuarkusCodestartTestBuilder putData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public QuarkusCodestartTestBuilder putData(DataKey key, Object value) {
        this.data.put(key.key(), value);
        return this;
    }

    public QuarkusCodestartTestBuilder extensions(Collection<ArtifactCoords> extensions) {
        this.extensions.addAll(extensions);
        return this;
    }

    public QuarkusCodestartTestBuilder extension(ArtifactCoords extension) {
        this.extensions.add(extension);
        return this;
    }

    public QuarkusCodestartTestBuilder extension(ArtifactKey extension) {
        this.extensions.add(Extensions.toCoords(extension, null));
        return this;
    }

    public QuarkusCodestartTestBuilder addArtifacts(Collection<String> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public QuarkusCodestartTestBuilder quarkusCodestartCatalog(QuarkusCodestartCatalog quarkusCodestartCatalog) {
        this.quarkusCodestartCatalog = quarkusCodestartCatalog;
        return this;
    }

    public QuarkusCodestartTestBuilder extensionCatalog(ExtensionCatalog extensionCatalog) {
        this.extensionCatalog = extensionCatalog;
        return this;
    }

    public QuarkusCodestartTestBuilder standaloneExtensionCatalog() {
        return this.standaloneExtensionCatalog(null, null);
    }

    public QuarkusCodestartTestBuilder standaloneExtensionCatalog(String quarkusBomGroupId, String quarkusBomVersion) {
        try {
            String buildWithQuarkusCoreVersion = null;
            final ArrayList<Extension> extensions = new ArrayList<>();
            for (URL url : Collections
                    .list(Thread.currentThread().getContextClassLoader().getResources("META-INF/quarkus-extension.yaml"))) {
                final ObjectMapper mapper = JsonCatalogMapperHelper.initMapper(new YAMLMapper());
                final JsonExtension extension = ResourceLoaders.processAsPath(url, path -> {
                    try {
                        return JsonCatalogMapperHelper.deserialize(mapper, path, JsonExtension.class);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                extensions.add(extension);
                if (buildWithQuarkusCoreVersion == null) {
                    buildWithQuarkusCoreVersion = getBuiltWithQuarkusCore(extension);
                }
            }
            Objects.requireNonNull(buildWithQuarkusCoreVersion, "quarkus version not found in extensions");
            String quarkusVersion = quarkusBomVersion != null ? quarkusBomVersion : buildWithQuarkusCoreVersion;
            enableRegistryClientTestConfig(quarkusBomGroupId != null ? quarkusBomGroupId : "io.quarkus", quarkusVersion);
            final ExtensionCatalog extensionCatalog = QuarkusProjectHelper
                    .getExtensionCatalog(quarkusVersion);
            disableRegistryClientTestConfig();
            if (!(extensionCatalog instanceof JsonExtensionCatalog)) {
                throw new IllegalStateException("Problem with the given ExtensionCatalog type");
            }
            extensions.addAll(extensionCatalog.getExtensions());
            ((JsonExtensionCatalog) extensionCatalog).setExtensions(extensions);
            this.extensionCatalog = extensionCatalog;
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading standalone extension catalog", e);
        }
        return this;
    }

    public QuarkusCodestartTest build() {
        return new QuarkusCodestartTest(this);
    }
}
