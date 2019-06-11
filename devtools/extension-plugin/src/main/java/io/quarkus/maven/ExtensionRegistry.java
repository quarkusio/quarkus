package io.quarkus.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.project.MavenProject;

import io.quarkus.dependencies.Extension;

/**
 * @author <a href="http://kenfinnigan.me">Ken Finnigan</a>
 */
class ExtensionRegistry {
    private static final String EXTENSION_NAME_PROPERTY_NAME = "quarkus.extension.name";
    private static final String EXTENSION_DESC_PROPERTY_NAME = "quarkus.extension.desc";
    private static final String EXTENSION_LABELS_PROPERTY_NAME = "quarkus.extension.labels";
    private static final String EXTENSION_INTERNAL_PROPERTY_NAME = "quarkus.extension.internal";

    private Map<Key, Extension> extensionRegistry = new HashMap<>();

    static final ExtensionRegistry INSTANCE = new ExtensionRegistry();

    private ExtensionRegistry() {

    }

    Extension of(MavenProject project) {
        if (project == null) {
            return null;
        }

        Key key = Key.of(project);
        if (extensionRegistry.containsKey(key)) {
            return extensionRegistry.get(key);
        }

        Extension extension = build(project);
        extensionRegistry.put(key, extension);
        return extension;
    }

    private Extension build(MavenProject project) {
        Extension extension = new Extension(project.getGroupId(), project.getArtifactId(), project.getVersion());

        String name = project.getProperties().getProperty(EXTENSION_NAME_PROPERTY_NAME);
        extension.setName(name == null ? project.getName() : name);

        String desc = project.getProperties().getProperty(EXTENSION_DESC_PROPERTY_NAME);
        extension.setDescription(desc == null ? project.getDescription() : desc);

        String labels = project.getProperties().getProperty(EXTENSION_LABELS_PROPERTY_NAME);
        if (labels != null) {
            extension.setLabels(labels.split(","));
        }

        String internal = project.getProperties().getProperty(EXTENSION_INTERNAL_PROPERTY_NAME);
        if (internal != null && internal.equals("true")) {
            extension.setInternal(true);
        }

        return extension;
    }

    private static class Key {
        private final String gav;

        Key(String groupId, String artifactId, String version) {
            this.gav = groupId + ":" + artifactId + ":" + version;
        }

        String gav() {
            return this.gav;
        }

        @Override
        public int hashCode() {
            return gav().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Key && gav().equals(((Key) obj).gav());
        }

        @Override
        public String toString() {
            return gav();
        }

        static Key of(MavenProject project) {
            return new Key(project.getGroupId(), project.getArtifactId(), project.getVersion());
        }
    }
}
