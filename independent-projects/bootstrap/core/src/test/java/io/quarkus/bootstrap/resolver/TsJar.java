package io.quarkus.bootstrap.resolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsJar implements TsArtifact.ContentProvider {

    private Path target;
    private Map<String, String> content = Collections.emptyMap();
    private Map<String, Path> paths = Collections.emptyMap();

    public TsJar(Path target) {
        this.target = target;
    }

    public TsJar() {
    }

    @Override
    public Path getPath(Path workDir) throws IOException {
        if (target == null) {
            target = workDir.resolve(UUID.randomUUID().toString());
        } else if(Files.exists(target)) {
            return target;
        }
        try (FileSystem zip = openZip()) {
            if (!content.isEmpty()) {
                for (Map.Entry<String, String> entry : content.entrySet()) {
                    final Path p = zip.getPath(entry.getKey());
                    Files.createDirectories(p.getParent());
                    try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                        writer.write(entry.getValue());
                    }
                }
            }
            if(!paths.isEmpty()) {
                for (Map.Entry<String, Path> entry : paths.entrySet()) {
                    final Path p = zip.getPath(entry.getKey());
                    Files.createDirectories(p.getParent());
                    IoUtils.copy(entry.getValue(), p);
                }
            }
        } catch (Throwable t) {
            throw t;
        }
        return target;
    }

    private String getKey(String... path) {
        if(path.length == 1) {
            return path[0];
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(path[0]);
        for(int i = 1; i < path.length; ++i) {
            buf.append('/').append(path[i]);
        }
        return buf.toString();
    }

    private void addContent(String content, String... path) {
        if(this.content.isEmpty()) {
            this.content = new HashMap<>(1);
        }
        this.content.put(getKey(path), content);
    }

    public TsJar addEntry(Path content, String... path) {
        if(paths.isEmpty()) {
            paths = new HashMap<>(1);
        }
        paths.put(getKey(path), content);
        return this;
    }

    public TsJar addEntry(String content, String... path) {
        addContent(content, path);
        return this;
    }

    public TsJar addEntry(Properties props, String... path) {
        final StringWriter writer = new StringWriter();
        try {
            props.store(writer, "Written by TsJarBuilder");
        } catch(IOException e) {
            throw new IllegalStateException("Failed to serialize properties", e);
        }
        addContent(writer.getBuffer().toString(), path);
        return this;
    }

    public TsJar addMavenMetadata(TsArtifact artifact, Path pomXml) {
        final Properties props = new Properties();
        props.setProperty("groupId", artifact.groupId);
        props.setProperty("artifactId", artifact.artifactId);
        props.setProperty("version", artifact.version);
        addEntry(props, "META-INF", "maven", artifact.groupId, artifact.artifactId, "pom.properties");
        addEntry(pomXml, "META-INF", "maven", artifact.groupId, artifact.artifactId, "pom.xml");
        return this;
    }

    private FileSystem openZip() throws IOException {
        final Instant buildTime = Instant.parse("2006-12-03T10:15:25.35Z");
        if(Files.exists(target)) {
            return ZipUtils.newFileSystem(target);
        }
        Files.createDirectories(target.getParent());
        return ZipUtils.newZip(target, buildTime);
    }
}
