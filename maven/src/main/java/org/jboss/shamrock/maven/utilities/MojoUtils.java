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

package org.jboss.shamrock.maven.utilities;


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * @author kameshs
 */
public class MojoUtils {

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private MojoUtils() {
        // Avoid direct instantiation
    }

    /**
     * Checks whether or not the given project has a plugin with the given key. The key is given using the
     * "groupId:artifactId" syntax.
     *
     * @param project   the project
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
     * @param project    - the project to check existence of dependency
     * @param groupId    - the dependency groupId
     * @param artifactId - the dependency artifactId
     * @return true if the project has the dependency
     */
    public static boolean hasDependency(MavenProject project, String groupId, String artifactId) {

        Optional<Dependency> dep = project.getDependencies().stream()
                .filter(d -> groupId.equals(d.getGroupId())
                        && artifactId.equals(d.getArtifactId())).findFirst();

        return dep.isPresent();
    }

    private static void loadProperties() {
        URL url = MojoUtils.class.getClassLoader().getResource("shamrock-maven-plugin.properties");
        Objects.requireNonNull(url);
        try (InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Invalid packaging of the shamrock-maven-plugin, the shamrock-maven-plugin" +
                    ".properties file cannot be read", e);
        }
    }

    public static String getVersion(String key) {
        return properties.getProperty(key);
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
     * Defines the plugin without its version or dependencies.
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId) {
        return plugin(groupId, artifactId, null);
    }

    /**
     * Defines a plugin without dependencies.
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @param version    The plugin version
     * @return The plugin instance
     */
    public static Plugin plugin(String groupId, String artifactId, String version) {
        return plugin(groupId, artifactId, version, Collections.<Dependency>emptyList());
    }

    /**
     * Defines a plugin.
     *
     * @param groupId      The group id
     * @param artifactId   The artifact id
     * @param version      The plugin version
     * @param dependencies The plugin dependencies
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
            for(Attribute attribute : attributes.attributes) {
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

        public Attributes(Attribute ... attributes) {
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
