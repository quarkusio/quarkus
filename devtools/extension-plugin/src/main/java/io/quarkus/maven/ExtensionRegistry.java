/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

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
