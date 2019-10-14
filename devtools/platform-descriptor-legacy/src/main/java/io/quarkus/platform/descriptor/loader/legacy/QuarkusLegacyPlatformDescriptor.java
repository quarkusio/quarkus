package io.quarkus.platform.descriptor.loader.legacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.tools.MessageWriter;

public class QuarkusLegacyPlatformDescriptor implements QuarkusPlatformDescriptor {

    private final ClassLoader cl;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final List<Extension> extensions;
    private final List<Dependency> managedDeps;
    private final MessageWriter log;

    public QuarkusLegacyPlatformDescriptor(ClassLoader cl, MessageWriter log) {
        this.cl = cl;
        this.log = log;

        // Resolve the BOM model
        InputStream is = cl.getResourceAsStream("quarkus-bom/pom.xml");
        if (is == null) {
            throw new RuntimeException("Failed to locate quarkus-bom/pom.xml");
        }
        final Model bomModel;
        try {
            bomModel = new MavenXpp3Reader().read(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse application POM model", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        groupId = resolveGroupId(bomModel);
        artifactId = bomModel.getArtifactId();
        version = resolveVersion(bomModel);
        managedDeps = bomModel.getDependencyManagement().getDependencies(); // This is raw but that's exactly how it used to be

        // Load extensions
        is = cl.getResourceAsStream("extensions.json");
        if (is == null) {
            throw new RuntimeException("Failed to locate quarkus-bom/pom.xml");
        }
        ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        try {
            extensions = mapper.readValue(
                    is,
                    new TypeReference<List<Extension>>() {
                        // Do nothing.
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed parse extensions.json", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public String getBomGroupId() {
        return groupId;
    }

    @Override
    public String getBomArtifactId() {
        return artifactId;
    }

    @Override
    public String getBomVersion() {
        return version;
    }

    @Override
    public String getQuarkusVersion() {
        return version;
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
        log.debug("[Legacy Quarkus Platform Descriptor] Loading Quarkus project template %s", name);
        InputStream is = cl.getResourceAsStream(name);
        if (is == null) {
            throw new RuntimeException("Failed to locate template " + name);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template " + name, e);
        }
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        log.debug("[Legacy Quarkus Platform Descriptor] Loading Quarkus platform resource %s", name);
        InputStream is = cl.getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Failed to locate resource " + name);
        }
        try {
            return consumer.handle(is);
        } finally {
            is.close();
        }
    }

    private static String resolveGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId != null) {
            return groupId;
        }
        groupId = model.getParent() == null ? null : model.getParent().getGroupId();
        if (groupId == null) {
            throw new IllegalStateException("Failed to resolve groupId for the platform BOM");
        }
        return groupId;
    }

    private static String resolveVersion(Model model) {
        String version = model.getVersion();
        if (version != null) {
            return version;
        }
        version = model.getParent() == null ? null : model.getParent().getVersion();
        if (version == null) {
            throw new IllegalStateException("Failed to resolve version for the platform BOM");
        }
        return version;
    }
}
