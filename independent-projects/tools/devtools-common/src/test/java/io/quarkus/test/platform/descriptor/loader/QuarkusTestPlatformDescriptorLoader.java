package io.quarkus.test.platform.descriptor.loader;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.ResourcePathConsumer;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.ResourceLoaders;
import io.quarkus.platform.tools.ToolsConstants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class QuarkusTestPlatformDescriptorLoader
        implements QuarkusPlatformDescriptorLoader<QuarkusPlatformDescriptor, QuarkusPlatformDescriptorLoaderContext> {

    private static final List<Extension> extensions = new ArrayList<>();
    private static final Properties quarkusProps;

    private static final String quarkusVersion;
    private static final List<Category> categories = new ArrayList<>();

    private String groupId = "io.quarkus";
    private String artifactId = "quarkus-bom";
    private String version;

    private static void addCategories() {
        addCategory("web", "Web");
        addCategory("data", "Data");
        addCategory("messaging", "Messaging");
        addCategory("core", "Core");
        addCategory("reactive", "Reactive");
        addCategory("cloud", "Cloud");
    }

    private static void addExtensions() {
        addExtension("quarkus-agroal", "Agroal");
        addExtension("quarkus-arc", "Arc");
        addExtension("quarkus-kotlin", "Kotlin", "url://", "kotlin");
        addExtension("quarkus-scala", "Scala", "url://", "scala");
        addExtension("quarkus-config-yaml", "Config Yaml", "url://", "config-yaml");
        addExtension("quarkus-hibernate-orm-panache", "Hibernate ORM Panache");
        addExtension("quarkus-hibernate-search-orm-elasticsearch", "Elasticsearch");
        addExtension("quarkus-hibernate-validator", "Hibernate Validator");
        addExtension("quarkus-jdbc-postgresql", "JDBC PostreSQL");
        addExtension("quarkus-jdbc-h2", "JDBC H2");
        addExtension("quarkus-resteasy", "RESTEasy", "https://quarkus.io/guides/rest-json", "resteasy");

        addExtension("quarkus-smallrye-reactive-messaging", "SmallRye Reactive Messaging");
        addExtension("quarkus-smallrye-reactive-streams-operators", "SmallRye Reactive Streams Operators");
        addExtension("quarkus-smallrye-opentracing", "SmallRye Opentracing");
        addExtension("quarkus-smallrye-metrics", "SmallRye Metrics");
        addExtension("quarkus-smallrye-reactive-messaging-kafka", "SmallRye Reactive Messaging Kafka");
        addExtension("quarkus-smallrye-health", "SmallRye Health");
        addExtension("quarkus-smallrye-openapi", "SmallRye Open API");
        addExtension("quarkus-smallrye-jwt", "SmallRye JWT");
        addExtension("quarkus-smallrye-context-propagation", "SmallRye Context Propagation");
        addExtension("quarkus-smallrye-reactive-type-converters", "SmallRye Reactive Type Converters");
        addExtension("quarkus-smallrye-reactive-messaging-amqp", "SmallRye Reactive Messaging AMQP");
        addExtension("quarkus-smallrye-fault-tolerance", "SmallRye Fault Tolerance");

        addExtension("quarkus-vertx", "Vert.X");
    }

    private static void addExtension(String artifactId, String name) {
        addExtension(artifactId, name, "url://" + name);
    }

    private static void addExtension(String artifactId, String name, String guide) {
        addExtension(artifactId, name, guide, null);
    }

    private static void addExtension(String artifactId, String name, String guide, String codestart) {
        addExtension(new AppArtifactCoords("io.quarkus", artifactId, quarkusVersion), name, guide, codestart);
    }

    private static void addExtension(AppArtifactCoords coords, String name, String guide, String codestart) {
        addExtension(coords, name, guide, codestart, extensions);
    }

    public static void addExtension(AppArtifactCoords coords, String name, String guide, String codestart,
            List<Extension> extensions) {
        final Extension e = new Extension(coords.getGroupId(), coords.getArtifactId(), coords.getVersion())
                .setName(name)
                .setGuide(guide);
        if (codestart != null) {
            e.setCodestart(codestart);
        }
        extensions.add(e);
    }

    private static void addCategory(String id, String name) {
        addCategory(id, name, categories);
    }

    public static void addCategory(String id, String name, List<Category> categories) {
        Category cat = new Category();
        cat.setId(id);
        cat.setName(name);
        categories.add(cat);
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    static {
        try {
            quarkusProps = loadStaticResource("quarkus.properties", is -> {
                final Properties props = new Properties();
                props.load(is);
                return props;
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load quarkus.properties", e);
        }
        quarkusVersion = quarkusProps.getProperty(ToolsConstants.PROP_QUARKUS_CORE_VERSION);
        if (quarkusVersion == null) {
            throw new IllegalStateException(
                    ToolsConstants.PROP_QUARKUS_CORE_VERSION + " property is missing from quarkus.properties");
        }

        addCategories();
        addExtensions();
    }

    @Override
    public QuarkusPlatformDescriptor load(QuarkusPlatformDescriptorLoaderContext context) {
        return new QuarkusPlatformDescriptor() {

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
                return Objects.toString(version, quarkusVersion);
            }

            @Override
            public String getQuarkusVersion() {
                return Objects.toString(version, quarkusVersion);
            }

            @Override
            public List<Extension> getExtensions() {
                return extensions;
            }

            @Override
            public String getTemplate(String name) {
                try {
                    return loadResource(name, is -> {
                        final StringWriter writer = new StringWriter();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                                BufferedWriter bw = new BufferedWriter(writer)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                bw.write(line);
                                bw.newLine();
                            }
                        }
                        return writer.getBuffer().toString();
                    });
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load resource " + name, e);
                }
            }

            @Override
            public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
                return loadStaticResource(name, consumer);
            }

            @Override
            public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
                return loadStaticResourcePath(name, consumer);
            }

            @Override
            public List<Category> getCategories() {
                return categories;
            }
        };
    }

    private static <T> T loadStaticResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Failed to locate resource " + name + " on the classpath");
        }
        try {
            return consumer.consume(is);
        } finally {
            is.close();
        }
    }

    private static <T> T loadStaticResourcePath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        final File file = ResourceLoaders.getResourceFile(url, name);
        if (!Files.exists(file.toPath())) {
            throw new IOException("Failed to locate resource " + name + " on the classpath");
        }
        return consumer.consume(file.toPath());
    }
}
