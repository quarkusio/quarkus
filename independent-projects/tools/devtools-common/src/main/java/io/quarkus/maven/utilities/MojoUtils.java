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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

    public static final String TEMPLATE_PROPERTY_QUARKUS_VERSION_NAME = "quarkus.version";
    public static final String TEMPLATE_PROPERTY_QUARKUS_VERSION_VALUE = toPropExpr(TEMPLATE_PROPERTY_QUARKUS_VERSION_NAME);

    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_GROUP_ID_NAME = "quarkus.platform.group-id";
    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_GROUP_ID_VALUE = toPropExpr(
            TEMPLATE_PROPERTY_QUARKUS_PLATFORM_GROUP_ID_NAME);

    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_NAME = "quarkus.platform.artifact-id";
    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE = toPropExpr(
            TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_NAME);

    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_NAME = "quarkus.platform.version";
    public static final String TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_VALUE = toPropExpr(
            TEMPLATE_PROPERTY_QUARKUS_PLATFORM_VERSION_NAME);

    public static final String TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_NAME = "quarkus-plugin.version";
    public static final String TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE = toPropExpr(
            TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_NAME);

    private static String toPropExpr(String name) {
        return "${" + name + "}";
    }

    private MojoUtils() {
        // Avoid direct instantiation
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
            res.setGroupId(segments[0].toLowerCase());
            res.setArtifactId(segments[1].toLowerCase());
            if (segments.length >= 3 && !segments[2].isEmpty()) {
                res.setVersion(segments[2]);
            }
            if (segments.length >= 4) {
                res.setClassifier(segments[3].toLowerCase());
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
        final Properties props = model.getProperties();
        // until we can preserve the user ordering, it's better to stick to the alphabetical one
        if (!props.isEmpty() && !(props instanceof SortedProperties)) {
            final Properties sorted = new SortedProperties();
            sorted.putAll(props);
            model.setProperties(sorted);
        }
        try (OutputStream stream = fileOutputStream) {
            new MavenXpp3Writer().write(stream, model);
        }
    }

    public static String credentials(final Dependency d) {
        return String.format("%s:%s", d.getGroupId(), d.getArtifactId());
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
        return getResourceOrigin(cls.getClassLoader(), cls.getName().replace('.', '/') + ".class");
    }

    public static Path getResourceOrigin(ClassLoader cl, final String name) throws IOException {
        URL url = cl.getResource(name);
        if (url == null) {
            throw new IOException("Failed to locate the origin of " + name);
        }
        String classLocation = url.toExternalForm();
        if (url.getProtocol().equals("jar")) {
            classLocation = classLocation.substring(4, classLocation.length() - name.length() - 2);
        } else {
            classLocation = classLocation.substring(0, classLocation.length() - name.length());
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
}
