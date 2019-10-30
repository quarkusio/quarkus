package io.quarkus.maven.utilities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

/**
 * @author kameshs
 */
public class MojoUtils {

    public static final String JAVA_FILE_EXTENSION = ".java";
    public static final String KOTLIN_FILE_EXTENSION = ".kt";
    public static final String SCALA_FILE_EXTENSION = ".scala";

    public static final String JAVA_EXTENSION_NAME = "java";
    public static final String KOTLIN_EXTENSION_NAME = "kotlin";
    public static final String SCALA_EXTENSION_NAME = "scala";

    public static final String QUARKUS_VERSION_PROPERTY_NAME = "quarkus.version";
    public static final String QUARKUS_VERSION_PROPERTY = "${" + QUARKUS_VERSION_PROPERTY_NAME + "}";

    private static Properties properties;

    private static QuarkusPlatformDescriptor platformDescr;

    private static QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr == null ? platformDescr = QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor() : platformDescr;
    }

    private static Properties getProperties() {
        if(properties == null) {
            try {
                properties = getPlatformDescriptor().loadResource("quarkus.properties", is -> {
                    final Properties props = new Properties();
                    props.load(is);
                    return props;
                });
            } catch (IOException e) {
                throw new IllegalStateException("The quarkus.properties file cannot be read", e);
            }
        }
        return properties;
    }

    private MojoUtils() {
        // Avoid direct instantiation
    }

    public static Map<String, String> getAllProperties() {
        Map<String, String> all = new HashMap<>();
        getProperties().stringPropertyNames().forEach(s -> all.put(s, getProperties().getProperty(s)));
        return all;
    }

    public static String getPluginArtifactId() {
        return get("plugin-artifactId");
    }

    public static String getPluginGroupId() {
        return get("plugin-groupId");
    }

    public static String getPluginKey() {
        return MojoUtils.getPluginGroupId() + ":" + MojoUtils.getPluginArtifactId();
    }

    public static String getPluginVersion() {
        return getPlatformDescriptor().getQuarkusVersion();
    }

    public static String getBomArtifactId() {
        return getPlatformDescriptor().getBomArtifactId();
    }

    public static String getBomGroupId() {
        return getPlatformDescriptor().getBomGroupId();
    }

    public static String getBomVersion() {
        return getPlatformDescriptor().getBomVersion();
    }

    public static String getBomVersionForTemplate(String defaultValue) {
        final String v = getBomVersion();
        if(v.equals(getQuarkusVersion())) {
            // this might not always work but at this point we're assuming the bom is coming from Quarkus itself
            return defaultValue;
        }
        return v;
    }

    public static String getQuarkusVersion() {
        return getPlatformDescriptor().getQuarkusVersion();
    }

    public static String getProposedMavenVersion() {
        return get("proposed-maven-version");
    }

    public static String getMavenWrapperVersion() {
        return get("maven-wrapper-version");
    }

    public static String getGradleWrapperVersion() {
        return get("gradle-wrapper-version");
    }

    public static String get(String key) {
        return getProperties().getProperty(key);
    }

    /**
     * Checks whether the project has the dependency
     *
     * @param model - the project to check existence of dependency
     * @param groupId - the dependency groupId
     * @param artifactId - the dependency artifactId
     * @return true if the project has the dependency
     */
    public static boolean hasDependency(Model model, String groupId, String artifactId) {
        return model.getDependencies().stream()
                .anyMatch(d -> groupId.equals(d.getGroupId())
                        && artifactId.equals(d.getArtifactId()));
    }

    public static Dependency parse(String dependency) {
        Dependency res = new Dependency();
        String[] segments = dependency.split(":");
        if (segments.length >= 2) {
            res.setGroupId(segments[0]);
            res.setArtifactId(segments[1]);
            if (segments.length >= 3 && !segments[2].isEmpty()) {
                res.setVersion(segments[2]);
            }
            if (segments.length >= 4) {
                res.setClassifier(segments[3]);
            }
            return res;
        } else {
            throw new IllegalArgumentException("Invalid dependency description '" + dependency + "'");
        }
    }

    /**
     * Builds the configuration for the goal using Elements
     *
     * @param elements A list of elements for the configuration section
     * @return The elements transformed into the Maven-native XML format
     */
    public static Xpp3Dom configuration(Element... elements) {
        Xpp3Dom dom = new Xpp3Dom("configuration");
        for (Element e : elements) {
            dom.addChild(e.toDom());
        }
        return dom;
    }

    /**
     * Defines the plugin without its version or extensions.
     *
     * @param groupId The group id
     * @param artifactId The artifact id
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId) {
        return plugin(groupId, artifactId, null);
    }

    /**
     * Defines a plugin without extensions.
     *
     * @param groupId The group id
     * @param artifactId The artifact id
     * @param version The plugin version
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId, String version) {
        return plugin(groupId, artifactId, version, Collections.emptyList());
    }

    /**
     * Defines a plugin.
     *
     * @param groupId The group id
     * @param artifactId The artifact id
     * @param version The plugin version
     * @param dependencies The plugin extensions
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId, String version, List<Dependency> dependencies) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        plugin.setDependencies(dependencies);
        return plugin;
    }

    public static Model readPom(final File pom) throws IOException {
        return readPom(new FileInputStream(pom));
    }

    public static Model readPom(final InputStream resourceAsStream) throws IOException {
        try (InputStream stream = resourceAsStream) {
            return new MavenXpp3Reader().read(stream);
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static String[] readGavFromPom(final InputStream resourceAsStream) throws IOException {
        Model model = readPom(resourceAsStream);
        return new String[] { model.getGroupId(), model.getArtifactId(), model.getVersion() };
    }

    public static void write(Model model, File outputFile) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        write(model, fileOutputStream);
    }

    public static void write(Model model, OutputStream fileOutputStream) throws IOException {
        try (OutputStream stream = fileOutputStream) {
            new MavenXpp3Writer().write(stream, model);
        }
    }

    public static List<Extension> loadExtensions() {
        return QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor().getExtensions();
    }

    public static String credentials(final Dependency d) {
        return String.format("%s:%s", d.getGroupId(), d.getArtifactId());
    }

    public static boolean checkProjectForMavenBuildPlugin(MavenProject project) {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getGroupId().equals("io.quarkus")
                    && plugin.getArtifactId().equals("quarkus-maven-plugin")) {
                for (PluginExecution pluginExecution : plugin.getExecutions()) {
                    if (pluginExecution.getGoals().contains("build")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Element wrapper class for configuration elements
     */
    public static class Element {
        private final Element[] children;
        private final String name;
        private final String text;
        private final Attributes attributes;

        public Element(String name, Element... children) {
            this(name, null, new Attributes(), children);
        }

        public Element(String name, Attributes attributes, Element... children) {
            this(name, null, attributes, children);
        }

        public Element(String name, String text, Element... children) {
            this.name = name;
            this.text = text;
            this.children = children;
            this.attributes = new Attributes();
        }

        public Element(String name, String text, Attributes attributes, Element... children) {
            this.name = name;
            this.text = text;
            this.children = children;
            this.attributes = attributes;
        }

        public Xpp3Dom toDom() {
            Xpp3Dom dom = new Xpp3Dom(name);
            if (text != null) {
                dom.setValue(text);
            }
            for (Element e : children) {
                dom.addChild(e.toDom());
            }
            for (Attribute attribute : attributes.attributes) {
                dom.setAttribute(attribute.name, attribute.value);
            }

            return dom;
        }
    }

    /**
     * Collection of attributes wrapper class
     */
    public static class Attributes {
        private List<Attribute> attributes;

        public Attributes(Attribute... attributes) {
            this.attributes = Arrays.asList(attributes);
        }
    }

    /**
     * Attribute wrapper class
     */
    public static class Attribute {
        private final String name;
        private final String value;

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static String[] readGavFromSettingsGradle(ByteArrayInputStream buildFileInputStream, String[] gavIn) {
        String[] gavOut = Arrays.copyOf(gavIn, gavIn.length);
        try (Scanner scanner = new Scanner(buildFileInputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                String currentLine = scanner.nextLine();
                if (currentLine.startsWith("group")) {
                    gavOut[0] = currentLine.substring(currentLine.indexOf('\'') + 1, currentLine.lastIndexOf('\''));
                } else if (currentLine.startsWith("rootProject.name")) {
                    gavOut[1] = currentLine.substring(currentLine.indexOf('\'') + 1, currentLine.lastIndexOf('\''));
                } else if (currentLine.startsWith("version")) {
                    gavOut[2] = currentLine.substring(currentLine.indexOf('\'') + 1, currentLine.lastIndexOf('\''));
                }
            }
        }
        return gavOut;
    }

    /**
     * Returns the JAR or the root directory that contains the class file that is on the
     * classpath of the context classloader
     */
    public static Path getClassOrigin(Class<?> cls) throws IOException {
        final String pluginClassPath = cls.getName().replace('.', '/') + ".class";
        URL url = cls.getClassLoader().getResource(pluginClassPath);
        if (url == null) {
            throw new IOException("Failed to locate the origin of " + cls);
        }
        String classLocation = url.toExternalForm();
        if (url.getProtocol().equals("jar")) {
            classLocation = classLocation.substring(4, classLocation.length() - pluginClassPath.length() - 2);
        } else {
            classLocation = classLocation.substring(0, classLocation.length() - pluginClassPath.length());
        }
        return urlSpecToPath(classLocation);
    }

    private static Path urlSpecToPath(String urlSpec) throws IOException {
        try {
            return Paths.get(new URL(urlSpec).toURI());
        } catch (Throwable e) {
            throw new IOException(
                    "Failed to create an instance of " + Path.class.getName() + " from " + urlSpec, e);
        }
    }

    public static ArtifactResolver toJsonArtifactResolver(MavenArtifactResolver mvn) {
        return new ArtifactResolver() {

            @Override
            public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                    Function<Path, T> processor) {
                final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
                try {
                    return processor.apply(mvn.resolve(artifact).getArtifact().getFile().toPath());
                } catch (AppModelResolverException e) {
                    throw new IllegalStateException("Failed to resolve " + artifact, e);
                }
            }

            @Override
            public List<Dependency> getManagedDependencies(String groupId, String artifactId, String classifier, String type, String version) {
                final List<org.eclipse.aether.graph.Dependency> deps;
                Artifact a = new DefaultArtifact(groupId, artifactId, classifier, type, version);
                try {
                    deps = mvn.resolveDescriptor(a).getManagedDependencies();
                } catch (AppModelResolverException e) {
                    throw new IllegalStateException("Failed to resolve descriptor for " + a, e);
                }
                final List<Dependency> result = new ArrayList<>(deps.size());
                for(org.eclipse.aether.graph.Dependency dep : deps) {
                    a = dep.getArtifact();
                    final Dependency d = new Dependency();
                    d.setGroupId(a.getGroupId());
                    d.setArtifactId(a.getArtifactId());
                    d.setClassifier(a.getClassifier());
                    d.setType(a.getExtension());
                    d.setVersion(a.getVersion());
                    d.setOptional(dep.isOptional());
                    d.setScope(dep.getScope());
                    result.add(d);
                }
                return result;
            }
        };
    }
}
