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

/**
 * Builder for {@link QuarkusCodestartTest}
 */
public class QuarkusCodestartTestBuilder {
    public static final String DEFAULT_PACKAGE_FOR_TESTING = "ilove.quark.us";

    public Map<String, Object> data = new HashMap<>();
    public Collection<String> artifacts = new ArrayList<>();
    public String packageName = DEFAULT_PACKAGE_FOR_TESTING;
    BuildTool buildTool;
    Set<String> codestarts = new HashSet<>();
    Set<Language> languages;
    QuarkusCodestartCatalog quarkusCodestartCatalog;
    ExtensionCatalog extensionCatalog;
    Collection<ArtifactCoords> extensions = new ArrayList<>();

    /**
     * Set the package name to use in the generated projects
     * 
     * @param packageName
     * @return
     */
    public QuarkusCodestartTestBuilder packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * Set the list of codestarts that should be added to the generated projects
     * 
     * @param codestarts the list of codestarts
     * @return
     */
    public QuarkusCodestartTestBuilder codestarts(String... codestarts) {
        this.codestarts = new HashSet<>(Arrays.asList(codestarts));
        return this;
    }

    /**
     * Set the list of languages in which we should generated projects
     * 
     * @param languages the list of languages
     * @return
     */
    public QuarkusCodestartTestBuilder languages(Language... languages) {
        this.languages = new HashSet<>(Arrays.asList(languages));
        return this;
    }

    /**
     * Set the build tool to use for testing (default is maven)
     * 
     * @param buildTool
     * @return
     */
    public QuarkusCodestartTestBuilder buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    /**
     * Add some custom codestart data for testing
     * 
     * @param key
     * @param value
     * @return
     */
    public QuarkusCodestartTestBuilder putData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * Add some custom codestart data for testing
     * 
     * @param key
     * @param value
     * @return
     */
    public QuarkusCodestartTestBuilder putData(DataKey key, Object value) {
        this.data.put(key.key(), value);
        return this;
    }

    /**
     * Set the list of extensions (and their codestarts) that should be added to the generated projects
     * 
     * @param extensions the list of extensions
     * @return
     */
    public QuarkusCodestartTestBuilder extensions(Collection<ArtifactCoords> extensions) {
        this.extensions.addAll(extensions);
        return this;
    }

    /**
     * Set the extension (and its codestarts) that should be added to the generated projects
     * 
     * @param extension the extension
     * @return
     */
    public QuarkusCodestartTestBuilder extension(ArtifactCoords extension) {
        this.extensions.add(extension);
        return this;
    }

    /**
     * Set the extension (and its codestarts) that should be added to the generated projects
     * 
     * @param extension the extension
     * @return
     */
    public QuarkusCodestartTestBuilder extension(ArtifactKey extension) {
        this.extensions.add(Extensions.toCoords(extension, null));
        return this;
    }

    /**
     * Add artifacts which contains codestarts
     * 
     * @param artifacts the artifacts coords
     * @return
     */
    public QuarkusCodestartTestBuilder addArtifacts(Collection<String> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * Specify a custom quarkus codestart catalog, by default it will use the extension catalog to get it.
     * 
     * @param quarkusCodestartCatalog the quarkusCodestartCatalog
     * @return
     */
    public QuarkusCodestartTestBuilder quarkusCodestartCatalog(QuarkusCodestartCatalog quarkusCodestartCatalog) {
        this.quarkusCodestartCatalog = quarkusCodestartCatalog;
        return this;
    }

    /**
     * Specify a custom extensionCatalog, by default it will use the test registry to get it.
     * 
     * @param extensionCatalog
     * @return
     */
    public QuarkusCodestartTestBuilder extensionCatalog(ExtensionCatalog extensionCatalog) {
        this.extensionCatalog = extensionCatalog;
        return this;
    }

    /**
     * This should be use in standalone/quarkiverse extension to add the local extension as part of the catalog, so it can be
     * tested
     * 
     * @return
     */
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
