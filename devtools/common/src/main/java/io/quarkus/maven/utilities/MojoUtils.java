/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package io.quarkus.maven.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.dependencies.Extension;

/**
 * @author kameshs
 */
public class MojoUtils {

    public static final String JAVA_EXTENSION = ".java";
    public static final String KOTLIN_EXTENSION = ".kt";

    private static final String PLUGIN_VERSION_PROPERTY_NAME = "quarkus.version";
    public static final String QUARKUS_VERSION_PROPERTY = "${" + PLUGIN_VERSION_PROPERTY_NAME + "}";

    private static final Properties properties = new Properties();

    private MojoUtils() {
        // Avoid direct instantiation
    }

    static {
        loadProperties();
    }

    public static Map<String, String> getAllProperties() {
        Map<String, String> all = new HashMap<>();
        properties.stringPropertyNames().forEach(s -> all.put(s, properties.getProperty(s)));
        return all;
    }

    public static String getPluginArtifactId() {
        return get("plugin-artifactId");
    }

    public static String getPluginGroupId() {
        return get("plugin-groupId");
    }

    public static String getPluginVersion() {
        return get("plugin-version");
    }

    public static String getBomArtifactId() {
        return get("bom-artifactId");
    }

    public static String getProposedMavenVersion() {
        return get("proposed-maven-version");
    }

    public static String getMavenWrapperVersion() {
        return get("maven-wrapper-version");
    }

    private static void loadProperties() {
        URL url = MojoUtils.class.getClassLoader().getResource("quarkus.properties");
        Objects.requireNonNull(url);
        try (InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("The quarkus.properties file cannot be read", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Checks whether or not the given project has a plugin with the given key. The key is given using the
     * "groupId:artifactId" syntax.
     *
     * @param project the project
     * @param pluginKey the plugin
     * @return an Optional completed if the plugin is found.
     */
    public static Optional<Plugin> hasPlugin(MavenProject project, String pluginKey) {
        Optional<Plugin> optPlugin = project.getBuildPlugins().stream()
                .filter(plugin -> pluginKey.equals(plugin.getKey()))
                .findFirst();

        if (!optPlugin.isPresent() && project.getPluginManagement() != null) {
            optPlugin = project.getPluginManagement().getPlugins().stream()
                    .filter(plugin -> pluginKey.equals(plugin.getKey()))
                    .findFirst();
        }
        return optPlugin;
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
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .enable(JsonParser.Feature.ALLOW_COMMENTS)
                    .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
            List<Extension> extensions = mapper.readValue(
                    MojoUtils.class.getClassLoader().getResourceAsStream("extensions.json"),
                    new TypeReference<List<Extension>>() {
                        // Do nothing.
                    });
            //TODO This is temporary until "extensions.json" is the generated version
            extensions.forEach(e -> e.setVersion(MojoUtils.QUARKUS_VERSION_PROPERTY));
            return extensions;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the extensions.json file", e);
        }
    }

    public static String credentials(final Dependency d) {
        return String.format("%s:%s", d.getGroupId(), d.getArtifactId());
    }

    public static boolean checkProjectForMavenBuildPlugin(MavenProject project) {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getGroupId().equals(MojoUtils.getPluginGroupId())
                    && plugin.getArtifactId().equals(MojoUtils.getPluginArtifactId())) {
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

}
