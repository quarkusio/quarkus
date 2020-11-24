package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.ResourcePathConsumer;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;

public class QuarkusJsonPlatformDescriptor implements QuarkusPlatformDescriptor, Serializable {

    private String bomGroupId;
    private String bomArtifactId;
    private String bomVersion;
    private String quarkusVersion;

    private List<Extension> extensions = Collections.emptyList();
    private List<Category> categories = Collections.emptyList();
    private Map<String, Object> metadata = Collections.emptyMap();
    private transient ResourceLoader resourceLoader;
    private transient MessageWriter log;

    public QuarkusJsonPlatformDescriptor() {
    }

    public void setBom(QuarkusJsonPlatformBom bom) {
        bomGroupId = bom.groupId;
        bomArtifactId = bom.artifactId;
        bomVersion = bom.version;
    }

    public void setQuarkusCoreVersion(String quarkusVersion) {
        this.quarkusVersion = quarkusVersion;
    }

    public void setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    void setMessageWriter(MessageWriter log) {
        this.log = log;
    }

    void setQuarkusVersion(String quarkusVersion) {
        this.quarkusVersion = quarkusVersion;
    }

    private MessageWriter getLog() {
        return log == null ? log = MessageWriter.info() : log;
    }

    @Override
    public String getBomGroupId() {
        return bomGroupId;
    }

    @Override
    public String getBomArtifactId() {
        return bomArtifactId;
    }

    @Override
    public String getBomVersion() {
        return bomVersion;
    }

    @Override
    public String getQuarkusVersion() {
        return quarkusVersion;
    }

    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    @JsonIgnore
    public List<Dependency> getManagedDependencies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTemplate(String name) {
        getLog().debug("Loading Quarkus project template %s", name);
        if (resourceLoader == null) {
            throw new IllegalStateException("Resource loader has not been provided");
        }
        try {
            return resourceLoader.loadResource(name, is -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + name, e);
        }
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        getLog().debug("Loading Quarkus platform resource %s", name);
        if (resourceLoader == null) {
            throw new IllegalStateException("Resource loader has not been provided");
        }
        return resourceLoader.loadResource(name, consumer);
    }

    @Override
    public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        getLog().debug("Loading Quarkus platform resource %s", name);
        if (resourceLoader == null) {
            throw new IllegalStateException("Resource loader has not been provided");
        }
        return resourceLoader.loadResourceAsPath(name, consumer);
    }

    @Override
    public List<Category> getCategories() {
        return categories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuarkusJsonPlatformDescriptor that = (QuarkusJsonPlatformDescriptor) o;
        return bomGroupId.equals(that.bomGroupId) &&
                bomArtifactId.equals(that.bomArtifactId) &&
                bomVersion.equals(that.bomVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bomGroupId, bomArtifactId, bomVersion);
    }
}
