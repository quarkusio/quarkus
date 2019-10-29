package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

public class QuarkusJsonPlatformDescriptor implements QuarkusPlatformDescriptor {

    private String bomGroupId;
    private String bomArtifactId;
    private String bomVersion;
    private String quarkusVersion;

    private List<Extension> extensions = Collections.emptyList();
    private List<Dependency> managedDeps = Collections.emptyList();
    private List<Category> categories = Collections.emptyList();
    private ResourceLoader resourceLoader;
    private MessageWriter log;

    public QuarkusJsonPlatformDescriptor() {
    }

    public void setBom(QuarkusJsonPlatformBom bom) {
        bomGroupId = bom.groupId;
        bomArtifactId = bom.artifactId;
        bomVersion = bom.version;
    }

    public void setExtensions(List<Extension> extensions) {
        this.extensions = extensions;
    }

    void setManagedDependencies(List<Dependency> managedDeps) {
        this.managedDeps = managedDeps;
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
        return log == null ? log = new DefaultMessageWriter() : log;
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
    public List<Dependency> getManagedDependencies() {
        return managedDeps;
    }

    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public String getTemplate(String name) {
        getLog().debug("Loading Quarkus project template %s", name);
        if (resourceLoader == null) {
            throw new IllegalStateException("Resource loader has not been provided");
        }
        try (InputStream is = resourceLoader.getResourceAsStream(name)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve template " + name, e);
        }
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        getLog().debug("Loading Quarkus platform resource %s", name);
        if (resourceLoader == null) {
            throw new IllegalStateException("Resource loader has not been provided");
        }
        try (InputStream is = resourceLoader.getResourceAsStream(name)) {
            return consumer.handle(is);
        }
    }

    @Override
    public List<Category> getCategories() {
        return categories;
    }
}
