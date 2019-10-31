package io.quarkus.test.platform.descriptor.loader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;

public class QuarkusTestPlatformDescriptorLoader implements QuarkusPlatformDescriptorLoader<QuarkusPlatformDescriptor, QuarkusPlatformDescriptorLoaderContext> {

    private static final List<Extension> extensions = new ArrayList<>();
    private static final List<Dependency> bomDeps = new ArrayList<>();
    private static final Properties quarkusProps;
    private static final String quarkusVersion;

    private static void addExtension(String artifactId, String name) {
        addExtension(artifactId, name, "url://" + name);
    }

    private static void addExtension(String artifactId, String name, String guide) {
        addExtension("io.quarkus", artifactId, quarkusVersion, name, guide);
    }

    private static void addExtension(String groupId, String artifactId, String version, String name, String guide) {
        extensions.add(new Extension(groupId, artifactId, version).setName(name).setGuide(guide));

        final Dependency d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion(version);
        bomDeps.add(d);
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
        quarkusVersion = quarkusProps.getProperty("plugin-version");
        if(quarkusVersion == null) {
            throw new IllegalStateException("plugin-version property is missing from quarkus.properties");
        }

        addExtension("quarkus-agroal", "Agroal");
        addExtension("quarkus-arc", "Arc");
        addExtension("quarkus-hibernate-orm-panache", "Hibernate ORM Panache");
        addExtension("quarkus-hibernate-search-elasticsearch", "Elasticsearch");
        addExtension("quarkus-hibernate-validator", "Hibernate Validator");
        addExtension("quarkus-jdbc-postgresql", "JDBC PostreSQL");
        addExtension("quarkus-jdbc-h2", "JDBC H2");
        addExtension("quarkus-resteasy", "RESTEasy", "https://quarkus.io/guides/rest-json");

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

    @Override
    public QuarkusPlatformDescriptor load(QuarkusPlatformDescriptorLoaderContext context) {
        return new QuarkusPlatformDescriptor() {

            @Override
            public String getBomGroupId() {
                return "io.quarkus";
            }

            @Override
            public String getBomArtifactId() {
                return"quarkus-bom";
            }

            @Override
            public String getBomVersion() {
                return quarkusVersion;
            }

            @Override
            public String getQuarkusVersion() {
                return quarkusVersion;
            }

            @Override
            public List<Dependency> getManagedDependencies() {
                return bomDeps;
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
                        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                            BufferedWriter bw = new BufferedWriter(writer)) {
                            String line;
                            while((line = reader.readLine()) != null) {
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
			public List<Category> getCategories() {
				return new ArrayList<Category>();
			}};
    }

    private static <T> T loadStaticResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if(is == null) {
            throw new IOException("Failed to locate resource " + name + " on the classpath");
        }
        try {
            return consumer.consume(is);
        } finally {
            is.close();
        }
    }
}
