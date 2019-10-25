package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

import io.quarkus.bootstrap.util.ZipUtils;
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
    private Path templatesJar;
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

    void setTemplatesJar(Path templatesJar) {
        this.templatesJar = templatesJar;
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
        if (templatesJar == null) {
            return null;
        }
        try {
            if (Files.isDirectory(templatesJar)) {
                return readTemplate(name, templatesJar.resolve(name));
            }
            try (FileSystem fs = ZipUtils.newFileSystem(templatesJar)) {
                return readTemplate(name, fs.getPath(name));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve template " + name, e);
        }
    }

    private String readTemplate(String name, final Path p) throws IOException {
        if (!Files.exists(p)) {
            throw new RuntimeException("Failed to locate template " + name + " in " + templatesJar);
        }
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        getLog().debug("Loading Quarkus platform resource %s", name);
        try (FileSystem fs = FileSystems.newFileSystem(templatesJar, null)) {
            final Path p = fs.getPath(name);
            if (!Files.exists(p)) {
                throw new IOException("Failed to locate resource " + name);
            }
            try (InputStream is = Files.newInputStream(p)) {
                return consumer.handle(is);
            }
        }
    }
}
